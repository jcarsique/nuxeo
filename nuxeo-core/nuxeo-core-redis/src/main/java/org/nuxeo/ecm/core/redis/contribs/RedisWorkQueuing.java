/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis.contribs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.work.WorkManagerImpl;
import org.nuxeo.ecm.core.work.WorkQueueDescriptorRegistry;
import org.nuxeo.ecm.core.work.WorkQueuing;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.runtime.api.Framework;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.util.SafeEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * Implementation of a {@link WorkQueuing} storing {@link Work} instances in Redis.
 *
 * @since 5.8
 */
public class RedisWorkQueuing implements WorkQueuing {

    private static final Log log = LogFactory.getLog(RedisWorkQueuing.class);

    protected static final String UTF_8 = "UTF-8";

    /**
     * Global hash of Work instance id -> serialized Work instance.
     */
    protected static final String KEY_DATA = "data";

    /**
     * Global hash of Work instance id -> Work state. The completed state ( {@value #STATE_COMPLETED_B}) is followed by
     * a completion time in milliseconds.
     */
    protected static final String KEY_STATE = "state";

    /**
     * Per-queue list of suspended Work instance ids.
     */
    protected static final String KEY_SUSPENDED_PREFIX = "prev:";

    /**
     * Per-queue list of scheduled Work instance ids.
     */
    protected static final String KEY_SCHEDULED_PREFIX = "queue:";

    /**
     * Per-queue set of running Work instance ids.
     */
    protected static final String KEY_RUNNING_PREFIX = "run:";

    /**
     * Per-queue set of completed Work instance ids.
     */
    protected static final String KEY_COMPLETED_PREFIX = "done:";

    protected static final byte STATE_SCHEDULED_B = 'Q';

    protected static final byte STATE_CANCELED_B = 'X';

    protected static final byte STATE_RUNNING_B = 'R';

    protected static final byte STATE_COMPLETED_B = 'C';

    protected static final byte[] STATE_SCHEDULED = new byte[]{STATE_SCHEDULED_B};

    protected static final byte[] STATE_CANCELED = new byte[]{STATE_CANCELED_B};

    protected static final byte[] STATE_RUNNING = new byte[]{STATE_RUNNING_B};

    protected static final byte[] STATE_COMPLETED = new byte[]{STATE_COMPLETED_B};

    protected final WorkManagerImpl mgr;

    // @GuardedBy("this")
    protected Map<String, BlockingQueue<Runnable>> allScheduled = new HashMap<String, BlockingQueue<Runnable>>();

    protected RedisExecutor redisExecutor;

    protected RedisAdmin redisAdmin;

    protected String redisNamespace;

    protected String delCompletedSha;

    public RedisWorkQueuing(WorkManagerImpl mgr, WorkQueueDescriptorRegistry workQueueDescriptors) {
        this.mgr = mgr;
    }

    @Override
    public void init() {
        redisExecutor = Framework.getLocalService(RedisExecutor.class);
        redisAdmin = Framework.getService(RedisAdmin.class);
        redisNamespace = redisAdmin.namespace("work");
        try {
            for (String queueId : getSuspendedQueueIds()) {
                int n = scheduleSuspendedWork(queueId);
                log.info("Re-scheduling " + n + " work instances suspended from queue: " + queueId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            delCompletedSha = redisAdmin.load("org.nuxeo.ecm.core.redis", "del-completed");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BlockingQueue<Runnable> initScheduleQueue(String queueId) {
        if (allScheduled.containsKey(queueId)) {
            throw new IllegalStateException(queueId + " is already configured");
        }
        final BlockingQueue<Runnable> scheduled = newBlockingQueue(queueId);
        allScheduled.put(queueId, scheduled);
        return scheduled;
    }

    @Override
    public BlockingQueue<Runnable> getScheduledQueue(String queueId) {
        if (!allScheduled.containsKey(queueId)) {
            throw new IllegalStateException(queueId + " was not configured yet");
        }
        return allScheduled.get(queueId);
    }

    @Override
    public void workRunning(String queueId, Work work) {
        try {
            workSetRunning(queueId, work);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void workCompleted(String queueId, Work work) {
        try {
            workSetCompleted(queueId, work);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BlockingQueue<Runnable> newBlockingQueue(String queueId) {
        return new RedisBlockingQueue(queueId, this);
    }

    @Override
    public List<Work> listWork(String queueId, State state) {
        switch (state) {
            case SCHEDULED:
                return listScheduled(queueId);
            case RUNNING:
                return listRunning(queueId);
            case COMPLETED:
                return listCompleted(queueId);
            default:
                throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    @Override
    public List<String> listWorkIds(String queueId, State state) {
        if (state == null) {
            return listNonCompletedIds(queueId);
        }
        switch (state) {
            case SCHEDULED:
                return listScheduledIds(queueId);
            case RUNNING:
                return listRunningIds(queueId);
            case COMPLETED:
                return listCompletedIds(queueId);
            default:
                throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    protected List<Work> listScheduled(String queueId) {
        try {
            return listWorkList(scheduledKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<Work> listRunning(String queueId) {
        try {
            return listWorkSet(runningKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<Work> listCompleted(String queueId) {
        try {
            return listWorkSet(completedKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<String> listScheduledIds(String queueId) {
        try {
            return listWorkIdsList(scheduledKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<String> listRunningIds(String queueId) {
        try {
            return listWorkIdsSet(runningKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<String> listNonCompletedIds(String queueId) {
        List<String> list = listScheduledIds(queueId);
        list.addAll(listRunningIds(queueId));
        return list;
    }

    protected List<String> listCompletedIds(String queueId) {
        try {
            return listWorkIdsSet(completedKey(queueId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getQueueSize(String queueId, State state) {
        switch (state) {
            case SCHEDULED:
                return getScheduledSize(queueId);
            case RUNNING:
                return getRunningSize(queueId);
            case COMPLETED:
                return getCompletedSize(queueId);
            default:
                throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    protected int getScheduledSize(String queueId) {
        try {
            return getScheduledQueueSize(queueId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected int getRunningSize(String queueId) {
        try {
            return getRunningQueueSize(queueId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected int getCompletedSize(String queueId) {
        try {
            return getCompletedQueueSize(queueId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Work find(String workId, State state) {
        if (isWorkInState(workId, state)) {
            return getWork(bytes(workId));
        }
        return null;
    }

    @Override
    public boolean isWorkInState(String workId, State state) {
        State s = getWorkState(workId);
        if (state == null) {
            return s == State.SCHEDULED || s == State.RUNNING;
        }
        return s == state;
    }

    @Override
    public Work removeScheduled(String queueId, String workId) {
        try {
            return removeScheduledWork(queueId, workId);
        } catch (IOException cause) {
            throw new RuntimeException("Cannot remove scheduled work " + workId + " from " + queueId, cause);
        }
    }

    @Override
    public State getWorkState(String workId) {
        try {
            return getWorkStateInfo(workId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int setSuspending(String queueId) {
        try {
            int n = suspendScheduledWork(queueId);
            log.info("Suspending " + n + " work instances from queue: " + queueId);
            return n;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearCompletedWork(String queueId, long completionTime) {
        try {
            if (completionTime <= 0) {
                removeAllCompletedWork(queueId);
            } else {
                removeCompletedWork(queueId, completionTime);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * ******************** Redis Interface ********************
     */

    protected String string(byte[] bytes) {
        try {
            return new String(bytes, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected byte[] bytes(String string) {
        try {
            return string.getBytes(UTF_8);
        } catch (IOException e) {
            // cannot happen for UTF-8
            throw new RuntimeException(e);
        }
    }

    protected byte[] keyBytes(String prefix, String queueId) {
        return keyBytes(prefix + queueId);
    }

    protected byte[] keyBytes(String prefix) {
        return bytes(redisNamespace + prefix);
    }

    protected byte[] suspendedKey(String queueId) {
        return keyBytes(KEY_SUSPENDED_PREFIX, queueId);
    }

    protected byte[] scheduledKey(String queueId) {
        return keyBytes(KEY_SCHEDULED_PREFIX, queueId);
    }

    protected byte[] runningKey(String queueId) {
        return keyBytes(KEY_RUNNING_PREFIX, queueId);
    }

    protected byte[] completedKey(String queueId) {
        return keyBytes(KEY_COMPLETED_PREFIX, queueId);
    }

    protected String completedKeyString(String queueId) {
        return redisNamespace + KEY_COMPLETED_PREFIX + queueId;
    }

    protected byte[] stateKey() {
        return keyBytes(KEY_STATE);
    }

    protected String stateKeyString() {
        return redisNamespace + KEY_STATE;
    }

    protected byte[] dataKey() {
        return keyBytes(KEY_DATA);
    }

    protected String dataKeyString() {
        return redisNamespace + KEY_DATA;
    }

    protected byte[] serializeWork(Work work) throws IOException {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baout);
        out.writeObject(work);
        out.flush();
        out.close();
        return baout.toByteArray();
    }

    protected Work deserializeWork(byte[] workBytes) {
        if (workBytes == null) {
            return null;
        }
        InputStream bain = new ByteArrayInputStream(workBytes);
        try (ObjectInputStream in = new ObjectInputStream(bain)) {
            return (Work) in.readObject();
        } catch (RuntimeException cause) {
            throw cause;
        } catch (IOException | ClassNotFoundException cause) {
            throw new RuntimeException("Cannot deserialize work", cause);
        }
    }

    protected int getScheduledQueueSize(final String queueId) throws IOException {
        return redisExecutor.execute(new RedisCallable<Long>() {

            @Override
            public Long call(Jedis jedis) {
                return jedis.llen(scheduledKey(queueId));
            }

        }).intValue();
    }

    protected int getRunningQueueSize(final String queueId) throws IOException {
        return redisExecutor.execute(new RedisCallable<Long>() {

            @Override
            public Long call(Jedis jedis) {
                return jedis.scard(runningKey(queueId));
            }

        }).intValue();
    }

    protected int getCompletedQueueSize(final String queueId) throws IOException {
        return redisExecutor.execute(new RedisCallable<Long>() {

            @Override
            public Long call(Jedis jedis) {
                return jedis.scard(completedKey(queueId));
            }

        }).intValue();
    }

    /**
     * Persists a work instance and adds it to the scheduled queue.
     *
     * @param queueId the queue id
     * @param work the work instance
     * @throws IOException
     */
    public void addScheduledWork(final String queueId, Work work) throws IOException {
        log.debug("Add scheduled " + work);
        final byte[] workIdBytes = bytes(work.getId());

        // serialize Work
        final byte[] workBytes = serializeWork(work);

        redisExecutor.execute(new RedisCallable<Void>() {

            @Override
            public Void call(Jedis jedis) {
                Pipeline pipe = jedis.pipelined();
                pipe.hset(dataKey(), workIdBytes, workBytes);
                pipe.hset(stateKey(), workIdBytes, STATE_SCHEDULED);
                pipe.lpush(scheduledKey(queueId), workIdBytes);
                pipe.sync();
                return null;
            }

        });
    }

    /**
     * Finds which queues have suspended work.
     *
     * @return a set of queue ids
     * @since 5.8
     */
    protected Set<String> getSuspendedQueueIds() throws IOException {
        return getQueueIds(KEY_SUSPENDED_PREFIX);
    }

    protected Set<String> getScheduledQueueIds() {
        try {
            return getQueueIds(KEY_SCHEDULED_PREFIX);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Set<String> getRunningQueueIds() {
        try {
            return getQueueIds(KEY_RUNNING_PREFIX);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getCompletedQueueIds() {
        try {
            return getQueueIds(KEY_COMPLETED_PREFIX);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds which queues have work for a given state prefix.
     *
     * @return a set of queue ids
     * @since 5.8
     */
    protected Set<String> getQueueIds(final String queuePrefix) throws IOException {
        return redisExecutor.execute(new RedisCallable<Set<String>>() {

            @Override
            public Set<String> call(Jedis jedis) throws IOException {
                int offset = keyBytes(queuePrefix).length;
                Set<byte[]> keys = jedis.keys(keyBytes(queuePrefix, "*"));
                Set<String> queueIds = new HashSet<String>(keys.size());
                for (byte[] bytes : keys) {
                    String queueId = new String(bytes, offset, bytes.length - offset, UTF_8);
                    queueIds.add(queueId);
                }
                return queueIds;
            }

        });
    }

    /**
     * Resumes all suspended work instances by moving them to the scheduled queue.
     *
     * @param queueId the queue id
     * @return the number of work instances scheduled
     * @since 5.8
     */
    public int scheduleSuspendedWork(final String queueId) throws IOException {
        return redisExecutor.execute(new RedisCallable<Integer>() {

            @Override
            public Integer call(Jedis jedis) throws IOException {
                for (int n = 0; ; n++) {
                    byte[] workIdBytes = jedis.rpoplpush(suspendedKey(queueId), scheduledKey(queueId));
                    if (workIdBytes == null) {
                        return Integer.valueOf(n);
                    }
                }
            }

        }).intValue();
    }

    /**
     * Suspends all scheduled work instances by moving them to the suspended queue.
     *
     * @param queueId the queue id
     * @return the number of work instances suspended
     * @since 5.8
     */
    public int suspendScheduledWork(final String queueId) throws IOException {
        return redisExecutor.execute(new RedisCallable<Integer>() {

            @Override
            public Integer call(Jedis jedis) throws IOException {
                for (int n = 0; ; n++) {
                    byte[] workIdBytes = jedis.rpoplpush(scheduledKey(queueId), suspendedKey(queueId));
                    if (workIdBytes == null) {
                        return n;
                    }
                }
            }
        }).intValue();
    }

    /**
     * Switches a work to state running.
     *
     * @param queueId the queue id
     * @param workId the work id
     */
    protected void workSetRunning(final String queueId, Work work) throws IOException {
        final byte[] workIdBytes = bytes(work.getId());
        redisExecutor.execute(new RedisCallable<Void>() {

            @Override
            public Void call(Jedis jedis) throws IOException {
                Pipeline pipe = jedis.pipelined();
                pipe.sadd(runningKey(queueId), workIdBytes);
                pipe.hset(stateKey(), workIdBytes, STATE_RUNNING);
                pipe.sync();
                return null;
            }
        });
    }

    /**
     * Switches a work to state completed, and saves its new state.
     *
     * @param queueId
     * @param id
     * @throws IOException
     */
    protected void workSetCompleted(final String queueId, final Work work) throws IOException {
        final byte[] workIdBytes = bytes(work.getId());
        final byte[] workBytes = serializeWork(work);
        redisExecutor.execute(new RedisCallable<Void>() {

            @Override
            public Void call(Jedis jedis) throws IOException {
                byte[] completedBytes = bytes(((char) STATE_COMPLETED_B) + String.valueOf(work.getCompletionTime()));
                Pipeline pipe = jedis.pipelined();
                // store (updated) content in hash
                pipe.hset(dataKey(), workIdBytes, workBytes);
                // remove key from running set
                pipe.srem(runningKey(queueId), workIdBytes);
                // put key in completed set
                pipe.sadd(completedKey(queueId), workIdBytes);
                // set state to completed
                pipe.hset(stateKey(), workIdBytes, completedBytes);
                pipe.sync();
                return null;
            }
        });
    }

    /**
     * Gets the work state.
     *
     * @param workId the work id
     * @return the state, or {@code null} if not found
     */
    protected State getWorkStateInfo(final String workId) throws IOException {
        final byte[] workIdBytes = bytes(workId);
        return redisExecutor.execute(new RedisCallable<State>() {

            @Override
            public State call(Jedis jedis) throws IOException {
                // get state
                byte[] bytes = jedis.hget(stateKey(), workIdBytes);
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                switch (bytes[0]) {
                    case STATE_SCHEDULED_B:
                        return State.SCHEDULED;
                    case STATE_CANCELED_B:
                        return State.CANCELED;
                    case STATE_RUNNING_B:
                        return State.RUNNING;
                    case STATE_COMPLETED_B:
                        return State.COMPLETED;
                    default:
                        log.error("Unknown work state: " + new String(bytes, UTF_8) + ", work: " + workId);
                        return null;
                }
            }
        });

    }

    protected List<String> listWorkIdsList(final byte[] queueBytes) throws IOException {
        return redisExecutor.execute(new RedisCallable<List<String>>() {

            @Override
            public List<String> call(Jedis jedis) throws IOException {
                List<byte[]> keys = jedis.lrange(queueBytes, 0, -1);
                List<String> list = new ArrayList<String>(keys.size());
                for (byte[] workIdBytes : keys) {
                    list.add(string(workIdBytes));
                }
                return list;
            }

        });
    }

    protected List<String> listWorkIdsSet(final byte[] queueBytes) throws IOException {
        return redisExecutor.execute(new RedisCallable<List<String>>() {

            @Override
            public List<String> call(Jedis jedis) throws IOException {

                Set<byte[]> keys = jedis.smembers(queueBytes);
                List<String> list = new ArrayList<String>(keys.size());
                for (byte[] workIdBytes : keys) {
                    list.add(string(workIdBytes));
                }
                return list;
            }

        });

    }

    protected List<Work> listWorkList(final byte[] queueBytes) throws IOException {
        return redisExecutor.execute(new RedisCallable<List<Work>>() {
            @Override
            public List<Work> call(Jedis jedis) throws IOException {
                List<byte[]> keys = jedis.lrange(queueBytes, 0, -1);
                List<Work> list = new ArrayList<Work>(keys.size());
                for (byte[] workIdBytes : keys) {
                    // get data
                    byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                    Work work = deserializeWork(workBytes);
                    list.add(work);
                }
                return list;
            }
        });
    }

    protected List<Work> listWorkSet(final byte[] queueBytes) throws IOException {
        return redisExecutor.execute(new RedisCallable<List<Work>>() {
            @Override
            public List<Work> call(Jedis jedis) {
                Set<byte[]> keys = jedis.smembers(queueBytes);
                List<Work> list = new ArrayList<Work>(keys.size());
                for (byte[] workIdBytes : keys) {
                    // get data
                    byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                    Work work = deserializeWork(workBytes);
                    list.add(work);
                }
                return list;
            }
        });
    }

    protected Work getWork(byte[] workIdBytes) {
        try {
            return getWorkData(workIdBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Work getWorkData(final byte[] workIdBytes) throws IOException {
        return redisExecutor.execute(new RedisCallable<Work>() {

            @Override
            public Work call(Jedis jedis) throws IOException {
                byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                return deserializeWork(workBytes);
            }

        });
    }

    /**
     * Removes first work from scheduled queue.
     *
     * @param queueId the queue id
     * @return the work, or {@code null} if the scheduled queue is empty
     */
    protected Work removeScheduledWork(final String queueId) throws IOException {
        return redisExecutor.execute(new RedisCallable<Work>() {

            @Override
            public Work call(Jedis jedis) throws IOException {
                // pop from queue
                byte[] workIdBytes = jedis.rpop(scheduledKey(queueId));
                if (workIdBytes == null) {
                    return null;
                }
                // get data
                byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                return deserializeWork(workBytes);
            }

        });
    }

    /**
     * Removes a given work from scheduled queue and set state to completed.
     *
     * @throws IOException
     */
    protected Work removeScheduledWork(final String queueId, final String workId) throws IOException {
        final byte[] workIdBytes = bytes(workId);
        return redisExecutor.execute(new RedisCallable<Work>() {

            @Override
            public Work call(Jedis jedis) throws IOException {
                // remove from queue
                Long n = jedis.lrem(scheduledKey(queueId), 0, workIdBytes);
                if (n == null || n.intValue() == 0) {
                    return null;
                }
                // set state to completed at current time
                byte[] completedBytes = bytes(String.valueOf(System.currentTimeMillis()));
                jedis.hset(stateKey(), workIdBytes, completedBytes);
                // get data
                byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                return deserializeWork(workBytes);
            }

        });
    }

    /**
     * Helper to call SSCAN but fall back on a custom implementation based on SMEMBERS if the backend (embedded) does
     * not support SSCAN.
     *
     * @since 7.3
     */
    public static class SScanner {

        // results of SMEMBERS for last key, in embedded mode
        protected List<String> smembers;

        protected ScanResult<String> sscan(Jedis jedis, String key, String cursor, ScanParams params) {
            ScanResult<String> scanResult;
            try {
                scanResult = jedis.sscan(key, cursor, params);
            } catch (Exception e) {
                // when testing with embedded fake redis, we may get an un-declared exception
                if (!(e.getCause() instanceof NoSuchMethodException)) {
                    throw e;
                }
                // no SSCAN available in embedded, do a full SMEMBERS
                if (smembers == null) {
                    Set<String> set = jedis.smembers(key);
                    smembers = new ArrayList<>(set);
                }
                Collection<byte[]> bparams = params.getParams();
                int count = 1000;
                for (Iterator<byte[]> it = bparams.iterator(); it.hasNext(); ) {
                    byte[] param = it.next();
                    if (param.equals(Protocol.Keyword.MATCH.raw)) {
                        throw new UnsupportedOperationException("MATCH not supported");
                    }
                    if (param.equals(Protocol.Keyword.COUNT.raw)) {
                        count = Integer.parseInt(SafeEncoder.encode(it.next()));
                    }
                }
                int pos = Integer.parseInt(cursor); // don't check range, callers are cool
                int end = Math.min(pos + count, smembers.size());
                int nextPos = end == smembers.size() ? 0 : end;
                scanResult = new ScanResult<>(String.valueOf(nextPos), smembers.subList(pos, end));
                if (nextPos == 0) {
                    smembers = null;
                }
            }
            return scanResult;
        }
    }

    /**
     * Don't pass more than approx. this number of parameters to Lua calls.
     */
    private static final int BATCH_SIZE = 5000;

    protected void removeAllCompletedWork(final String queueId) throws IOException {
        removeCompletedWork(queueId, 0);
    }

    protected void removeCompletedWork(final String queueId, final long completionTime) throws IOException {
        redisExecutor.execute(new RedisCallable<Void>() {

            @Override
            public Void call(Jedis jedis) throws IOException {
                String completedKey = completedKeyString(queueId);
                String stateKey = stateKeyString();
                String dataKey = dataKeyString();
                SScanner sscanner = new SScanner();
                ScanParams scanParams = new ScanParams().count(BATCH_SIZE);
                String cursor = "0";
                do {
                    ScanResult<String> scanResult = sscanner.sscan(jedis, completedKey, cursor, scanParams);
                    cursor = scanResult.getStringCursor();
                    List<String> workIds = scanResult.getResult();
                    if (!workIds.isEmpty()) {
                        // delete these works if before the completion time
                        List<String> keys = Arrays.asList(completedKey, stateKey, dataKey);
                        List<String> args = new ArrayList<String>(1 + workIds.size());
                        args.add(String.valueOf(completionTime));
                        args.addAll(workIds);
                        jedis.evalsha(delCompletedSha, keys, args);
                    }
                } while (!"0".equals(cursor));
                return null;
            }
        });
    }

}
