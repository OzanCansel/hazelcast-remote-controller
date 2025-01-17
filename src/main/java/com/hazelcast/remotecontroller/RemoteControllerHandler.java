package com.hazelcast.remotecontroller;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.management.ScriptEngineManagerContext;
import org.apache.thrift.TException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Locale;

public class RemoteControllerHandler implements RemoteController.Iface {

    private ClusterManager clusterManager;
    private HazelcastCloudManager cloudManager;

    public RemoteControllerHandler() {
        this.clusterManager = new ClusterManager();
    }

    @Override
    public boolean ping() throws TException {
        return clusterManager.ping();
    }

    @Override
    public boolean clean() throws TException {
        return clusterManager.clean();
    }

    @Override
    public boolean exit() throws TException {
        return clusterManager.clean();
    }

    @Override
    public Cluster createCluster(String hzVersion, String xmlconfig) throws TException {
        return clusterManager.createCluster(hzVersion, xmlconfig, false);
    }

    @Override
    public Cluster createClusterKeepClusterName(String hzVersion, String xmlconfig) throws ServerException, TException {
        return clusterManager.createCluster(hzVersion, xmlconfig, true);
    }

    @Override
    public Member startMember(String clusterId) throws TException {
        return clusterManager.startMember(clusterId);
    }

    @Override
    public boolean shutdownMember(String clusterId, String memberId) throws TException {
        return clusterManager.shutdownMember(clusterId, memberId);
    }

    @Override
    public boolean terminateMember(String clusterId, String memberId) throws TException {
        return clusterManager.terminateMember(clusterId, memberId);
    }

    @Override
    public boolean suspendMember(String clusterId, String memberId) throws TException {
        return clusterManager.suspendMember(clusterId, memberId);
    }

    @Override
    public boolean resumeMember(String clusterId, String memberId) throws TException {
        return clusterManager.resumeMember(clusterId, memberId);
    }

    @Override
    public boolean shutdownCluster(String clusterId) throws TException {
        return clusterManager.shutdownCluster(clusterId);
    }

    @Override
    public boolean terminateCluster(String clusterId) throws TException {
        return clusterManager.terminateCluster(clusterId);
    }

    @Override
    public Cluster splitMemberFromCluster(String memberId) throws TException {
        //TODO
        return null;
    }

    @Override
    public Cluster mergeMemberToCluster(String clusterId, String memberId) throws TException {
        //TODO
        return null;
    }

    @Override
    public void loginToHazelcastCloudUsingEnvironment() throws TException {
        if(cloudManager == null)
            cloudManager = new HazelcastCloudManager();
        cloudManager.loginToHazelcastCloudUsingEnvironment();
    }

    @Override
    public void loginToHazelcastCloud(String uri, String apiKey, String apiSecret) throws TException {
        if(cloudManager == null)
            cloudManager = new HazelcastCloudManager();
        cloudManager.loginToHazelcastCloud(uri, apiKey, apiSecret);
    }

    @Override
    public CloudCluster createHazelcastCloudStandardCluster(String hazelcastVersion, boolean isTlsEnabled) throws TException {
        return getCloudManager().createHazelcastCloudStandardCluster(hazelcastVersion, isTlsEnabled);
    }

    @Override
    public void setHazelcastCloudClusterMemberCount(String id, int totalMemberCount) throws TException {
        getCloudManager().setHazelcastCloudClusterMemberCount(id, totalMemberCount);
    }

    @Override
    public CloudCluster getHazelcastCloudCluster(String id) throws TException {
        return getCloudManager().getHazelcastCloudCluster(id);
    }

    @Override
    public CloudCluster stopHazelcastCloudCluster(String id) throws TException {
        return getCloudManager().stopHazelcastCloudCluster(id);
    }

    @Override
    public CloudCluster resumeHazelcastCloudCluster(String id) throws TException {
        return getCloudManager().resumeHazelcastCloudCluster(id);
    }

    @Override
    public void deleteHazelcastCloudCluster(String id) throws TException {
        getCloudManager().deleteHazelcastCloudCluster(id);
    }

    private HazelcastCloudManager getCloudManager() throws CloudException {
        if(cloudManager == null)
            throw new CloudException("It seems cloud manager is null, did you login?");
        return cloudManager;
    }

    @Override
    public Response executeOnController(String clusterId, String script, Lang lang) throws TException {
        //TODO
        ScriptEngineManager scriptEngineManager = ScriptEngineManagerContext.getScriptEngineManager();
        String engineName = lang.name().toLowerCase(Locale.ENGLISH);
        ScriptEngine engine = scriptEngineManager.getEngineByName(engineName);
        if (engine == null) {
            throw new IllegalArgumentException("Could not find ScriptEngine named:" + engineName);
        }
        
        // Interpret `clusterId` as null if it is empty
        // Because thrift cpp-bindings don't support null String.
        if (clusterId != null && !clusterId.isEmpty()) {
            int i = 0;
            for (HazelcastInstance instance : clusterManager.getCluster(clusterId).getInstances()) {
                engine.put("instance_" + i++, instance);
            }
        }
        Response response = new Response();
        try {
            engine.eval(script);
            Object result = engine.get("result");
            if (result instanceof Throwable) {
                response.setMessage(((Throwable) result).getMessage());
            } else if (result instanceof byte[]) {
                response.setResult((byte[]) result);
            } else if (result instanceof String) {
                response.setResult(((String) result).getBytes("utf-8"));
            }
            response.setSuccess(true);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
        }
        return response;
    }

}
