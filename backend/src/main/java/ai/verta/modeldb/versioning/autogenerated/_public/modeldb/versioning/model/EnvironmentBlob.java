// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;

public class EnvironmentBlob {
    public PythonEnvironmentBlob Python;
    public DockerEnvironmentBlob Docker;
    public List<EnvironmentVariablesBlob> EnvironmentVariables;
    public List<String> CommandLine;

    public EnvironmentBlob() {
        this.Python = null;
        this.Docker = null;
        this.EnvironmentVariables = null;
        this.CommandLine = null;
    }

    public EnvironmentBlob setPython(PythonEnvironmentBlob value) {
        this.Python = value;
        return this;
    }
    public EnvironmentBlob setDocker(DockerEnvironmentBlob value) {
        this.Docker = value;
        return this;
    }
    public EnvironmentBlob setEnvironmentVariables(List<EnvironmentVariablesBlob> value) {
        this.EnvironmentVariables = value;
        return this;
    }
    public EnvironmentBlob setCommandLine(List<String> value) {
        this.CommandLine = value;
        return this;
    }

    static public EnvironmentBlob fromProto(ai.verta.modeldb.versioning.EnvironmentBlob blob) {
        EnvironmentBlob obj = new EnvironmentBlob();
        {
            Function<ai.verta.modeldb.versioning.EnvironmentBlob,PythonEnvironmentBlob> f = x -> { return PythonEnvironmentBlob.fromProto(x.getPython()); };
            //PythonEnvironmentBlob.fromProto;
            if (f != null) {
                obj.Python = f.apply(blob);
            }
        }
        {
            Function<ai.verta.modeldb.versioning.EnvironmentBlob,DockerEnvironmentBlob> f = x -> { return DockerEnvironmentBlob.fromProto(x.getDocker()); };
            //DockerEnvironmentBlob.fromProto;
            if (f != null) {
                obj.Docker = f.apply(blob);
            }
        }
        {
            Function<ai.verta.modeldb.versioning.EnvironmentBlob,List<EnvironmentVariablesBlob>> f = x -> { return ((Function<List<ai.verta.modeldb.versioning.EnvironmentVariablesBlob>,List<EnvironmentVariablesBlob>>) y -> y.stream().map(z -> EnvironmentVariablesBlob.fromProto(z)).collect(Collectors.toList())).apply(x.getEnvironmentVariablesList()); };
            //((Function<List<ai.verta.modeldb.versioning.EnvironmentVariablesBlob>,List<EnvironmentVariablesBlob>>) y -> y.stream().map(z -> EnvironmentVariablesBlob.fromProto(z)).collect(Collectors.toList())).apply;
            if (f != null) {
                obj.EnvironmentVariables = f.apply(blob);
            }
        }
        {
            Function<ai.verta.modeldb.versioning.EnvironmentBlob,List<String>> f = x -> { return (x.getCommandLineList()); };
            //;
            if (f != null) {
                obj.CommandLine = f.apply(blob);
            }
        }
        return obj;
    }

    public void preVisitShallow(Visitor visitor) throws ModelDBException {
        visitor.preVisitEnvironmentBlob(this);
    }

    public void preVisitDeep(Visitor visitor) throws ModelDBException {
        this.preVisitShallow(visitor);
        visitor.preVisitDeepPythonEnvironmentBlob(this.Python);
        visitor.preVisitDeepDockerEnvironmentBlob(this.Docker);
        visitor.preVisitDeepListOfEnvironmentVariablesBlob(this.EnvironmentVariables);
        visitor.preVisitDeepListOfString(this.CommandLine);
    }

    public EnvironmentBlob postVisitShallow(Visitor visitor) throws ModelDBException {
        return visitor.postVisitEnvironmentBlob(this);
    }

    public EnvironmentBlob postVisitDeep(Visitor visitor) throws ModelDBException {
        this.Python = visitor.postVisitDeepPythonEnvironmentBlob(this.Python);
        this.Docker = visitor.postVisitDeepDockerEnvironmentBlob(this.Docker);
        this.EnvironmentVariables = visitor.postVisitDeepListOfEnvironmentVariablesBlob(this.EnvironmentVariables);
        this.CommandLine = visitor.postVisitDeepListOfString(this.CommandLine);
        return this.postVisitShallow(visitor);
    }
}
