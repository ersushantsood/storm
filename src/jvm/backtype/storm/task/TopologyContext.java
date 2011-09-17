package backtype.storm.task;

import backtype.storm.generated.Bolt;
import backtype.storm.generated.ComponentCommon;
import backtype.storm.generated.GlobalStreamId;
import backtype.storm.generated.Grouping;
import backtype.storm.generated.SpoutSpec;
import backtype.storm.generated.StateSpoutSpec;
import backtype.storm.generated.StormTopology;
import backtype.storm.state.ISubscribedState;
import backtype.storm.tuple.Fields;
import backtype.storm.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.NotImplementedException;
import org.json.simple.JSONValue;

/**
 * A TopologyContext is given to bolts and spouts in their "prepare" and "open"
 * methods, respectively. This object provides information about the component's
 * place within the topology, such as task ids, inputs and outputs, etc.
 *
 * <p>The TopologyContext is also used to declare ISubscribedState objects to
 * synchronize state with StateSpouts this object is subscribed to.</p>
 */
public class TopologyContext {
    private StormTopology _topology;
    private Map<Integer, Integer> _taskToComponent;
    private int _taskId;
    private Map<Integer, List<Integer>> _componentToTasks;
    private String _codeDir;
    private String _pidDir;
    private String _stormId;

    public TopologyContext(StormTopology topology, Map<Integer, Integer> taskToComponent, String stormId, String codeDir, String pidDir, int taskId) {
        _topology = topology;
        _taskToComponent = taskToComponent;
        _stormId = stormId;
        _taskId = taskId;
        _componentToTasks = new HashMap<Integer, List<Integer>>();
        _pidDir = pidDir;
        _codeDir = codeDir;
        for(Integer task: taskToComponent.keySet()) {
            int component = taskToComponent.get(task);
            List<Integer> curr = _componentToTasks.get(component);
            if(curr==null) curr = new ArrayList<Integer>();
            curr.add(task);
            _componentToTasks.put(component, curr);
        }
    }

    /**
     * All state from all subscribed state spouts streams will be synced with
     * the provided object.
     * 
     * <p>It is recommended that your ISubscribedState object is kept as an instance
     * variable of this object. The recommended usage of this method is as follows:</p>
     *
     * <p>
     * _myState = context.setAllSubscribedState(new MyState());
     * </p>
     * @param obj Provided ISubscribedState implementation
     * @return Returns the ISubscribedState object provided
     */
    public <T extends ISubscribedState> T setAllSubscribedState(T obj) {
        //check that only subscribed to one component/stream for statespout
        //setsubscribedstate appropriately
        throw new NotImplementedException();
    }


    /**
     * Synchronizes the default stream from the specified state spout component
     * id with the provided ISubscribedState object.
     *
     * <p>The recommended usage of this method is as follows:</p>
     * <p>
     * _myState = context.setSubscribedState(componentId, new MyState());
     * </p>
     *
     * @param componentId the id of the StateSpout component to subscribe to
     * @param obj Provided ISubscribedState implementation
     * @return Returns the ISubscribedState object provided
     */
    public <T extends ISubscribedState> T setSubscribedState(int componentId, T obj) {
        return setSubscribedState(componentId, Utils.DEFAULT_STREAM_ID, obj);
    }

    /**
     * Synchronizes the specified stream from the specified state spout component
     * id with the provided ISubscribedState object.
     *
     * <p>The recommended usage of this method is as follows:</p>
     * <p>
     * _myState = context.setSubscribedState(componentId, streamId, new MyState());
     * </p>
     *
     * @param componentId the id of the StateSpout component to subscribe to
     * @param streamId the stream to subscribe to
     * @param obj Provided ISubscribedState implementation
     * @return Returns the ISubscribedState object provided
     */
    public <T extends ISubscribedState> T setSubscribedState(int componentId, int streamId, T obj) {
        throw new NotImplementedException();
    }

    public ComponentType getComponentType(int componentId) {
        if(_topology.get_bolts().containsKey(componentId)) {
            return ComponentType.BOLT;
        }
        if(_topology.get_spouts().containsKey(componentId)) {
            return ComponentType.SPOUT;
        }
        if(_topology.get_state_spouts().containsKey(componentId)) {
            return ComponentType.STATE_SPOUT;
        }
        throw new IllegalArgumentException("Invalid component id: " + componentId);
    }

    public boolean isThisSpout() {
        return isSpout(getThisComponentId());
    }

    public boolean isSpout(int componentId) {
        return getComponentType(componentId) == ComponentType.SPOUT;
    }

    public boolean isThisBolt() {
        return isBolt(getThisComponentId());
    }

    public boolean isBolt(int componentId) {
        return getComponentType(componentId) == ComponentType.BOLT;
    }

    public boolean isThisStateSpout() {
        return isStateSpout(getThisComponentId());
    }

    public boolean isStateSpout(int componentId) {
        return getComponentType(componentId) == ComponentType.STATE_SPOUT;
    }

    /**
     * Gets the unique id assigned to this topology. The id is the storm name with a
     * unique nonce appended to it.
     * @return the storm id
     */
    public String getStormId() {
        return _stormId;
    }

    /**
     * Gets the task id of this task.
     * 
     * @return the task id
     */
    public int getThisTaskId() {
        return _taskId;
    }

    /**
     * Gets the Thrift object representing the topology.
     * 
     * @return the Thrift definition representing the topology
     */
    public StormTopology getRawTopology() {
        return _topology;
    }

    /**
     * Gets the component id for the specified task id. The component id maps
     * to a component id specified for a Spout or Bolt in the topology definition.
     *
     * @param taskId the task id
     * @return the component id for the input task id
     */
    public int getComponentId(int taskId) {
        return _taskToComponent.get(taskId);
    }

    /**
     * Gets the component id for this task. The component id maps
     * to a component id specified for a Spout or Bolt in the topology definition.
     * @return
     */
    public int getThisComponentId() {
        return getComponentId(_taskId);
    }

    /**
     * Gets the declared output fields for the specified stream id for the component
     * this task is a part of.
     */
    public Fields getThisOutputFields(int streamId) {
        return getComponentOutputFields(getThisComponentId(), streamId);
    }

    /**
     * Gets the set of streams declared for the component of this task.
     */
    public Set<Integer> getThisStreams() {
        return getComponentStreams(getThisComponentId());
    }

    /**
     * Gets the set of streams declared for the specified component.
     */
    public Set<Integer> getComponentStreams(int componentId) {
        return getComponentCommon(componentId).get_streams().keySet();
    }

    /**
     * Gets the task ids allocated for the given component id. The task ids are
     * always returned in ascending order.
     */
    public List<Integer> getComponentTasks(int componentId) {
        List<Integer> ret = _componentToTasks.get(componentId);
        if(ret==null) return new ArrayList<Integer>();
        else return new ArrayList<Integer>(ret);
    }

    /**
     * Gets the index of this task id in getComponentTasks(getThisComponentId()).
     * An example use case for this method is determining which task
     * accesses which resource in a distributed resource to ensure an even distribution.
     */
    public int getThisTaskIndex() {
        List<Integer> tasks = new ArrayList<Integer>(getComponentTasks(getThisComponentId()));
        Collections.sort(tasks);
        for(int i=0; i<tasks.size(); i++) {
            if(tasks.get(i) == getThisTaskId()) {
                return i;
            }
        }
        throw new RuntimeException("Fatal: could not find this task id in this component");
    }

    /**
     * Gets the declared output fields for the specified component/stream.
     */
    public Fields getComponentOutputFields(int componentId, int streamId) {
        return new Fields(getComponentCommon(componentId).get_streams().get(streamId).get_output_fields());
    }

    /**
     * Gets the declared inputs to this component.
     * 
     * @return A map from subscribed component/stream to the grouping subscribed with.
     */
    public Map<GlobalStreamId, Grouping> getThisSources() {
        return getSources(getThisComponentId());
    }

    public Set<Integer> getThisSourceComponents() {
        return getSourceComponents(getThisComponentId());
    }

    public Set<Integer> getSourceComponents(int componentId) {
        Map<GlobalStreamId, Grouping> sources = getSources(componentId);
        Set<Integer> ret = new HashSet<Integer>();
        if(sources!=null) {
            for(GlobalStreamId id: sources.keySet()) {
                ret.add(id.get_componentId());
            }
        }
        return ret;
    }
    
    /**
     * Gets the declared inputs to the specified component.
     *
     * @return A map from subscribed component/stream to the grouping subscribed with.
     */
    public Map<GlobalStreamId, Grouping> getSources(int componentId) {
        Bolt bolt = _topology.get_bolts().get(componentId);
        if(bolt==null) return null;
        return bolt.get_inputs();
    }

    /**
     * Gets information about who is consuming the outputs of this component, and how.
     *
     * @return Map from stream id to component id to the Grouping used.
     */
    public Map<Integer, Map<Integer, Grouping>> getThisTargets() {
        return getTargets(getThisComponentId());
    }

    /**
     * Gets information about who is consuming the outputs of the specified component,
     * and how.
     *
     * @return Map from stream id to component id to the Grouping used.
     */
    public Map<Integer, Map<Integer, Grouping>> getTargets(int componentId) {
        Map<Integer, Map<Integer, Grouping>> ret = new HashMap<Integer, Map<Integer, Grouping>>();
        for(int otherComponentId: _topology.get_bolts().keySet()) {
            Bolt bolt = _topology.get_bolts().get(otherComponentId);
            for(GlobalStreamId id: bolt.get_inputs().keySet()) {
                if(id.get_componentId()==componentId) {
                    Map<Integer, Grouping> curr = ret.get(id.get_streamId());
                    if(curr==null) curr = new HashMap<Integer, Grouping>();
                    curr.put(otherComponentId, bolt.get_inputs().get(id));
                    ret.put(id.get_streamId(), curr);
                }
            }
        }
        return ret;
    }

    public String toJSONString() {
        Map obj = new HashMap();
        obj.put("taskid", _taskId);
        obj.put("task->component", _taskToComponent);
        // TODO: jsonify StormTopology
        // at the minimum should send source info
        return JSONValue.toJSONString(obj);
    }

    private ComponentCommon getComponentCommon(int componentId) {
       Bolt bolt =  _topology.get_bolts().get(componentId);
       if(bolt!=null) {
           return bolt.get_common();
       }
       SpoutSpec spoutSpec = _topology.get_spouts().get(componentId);
       if(spoutSpec!=null) {
           return spoutSpec.get_common();
       }
       StateSpoutSpec stateSpoutSpec = _topology.get_state_spouts().get(componentId);
       if(stateSpoutSpec!=null) {
           return stateSpoutSpec.get_common();
       }
       throw new IllegalArgumentException("Could not find component common for " + componentId);
    }

    /**
     * Gets the location of the external resources for this worker on the
     * local filesystem. These external resources typically include bolts implemented
     * in other languages, such as Ruby or Python.
     */
    public String getCodeDir() {
        return _codeDir;
    }

    /**
     * If this task spawns any subprocesses, those subprocesses must immediately
     * write their PID to this directory on the local filesystem to ensure that
     * Storm properly destroys that process when the worker is shutdown.
     */
    public String getPIDDir() {
        return _pidDir;
    }

    /**
     * Gets a map from task id to component id.
     */
    public Map<Integer, Integer> getTaskToComponent() {
        return _taskToComponent;
    }
}