// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;

public class PythonEnvironmentBlob {
    public VersionEnvironmentBlob Version;
    public List<PythonRequirementEnvironmentBlob> Requirements;
    public List<PythonRequirementEnvironmentBlob> Constraints;

    public PythonEnvironmentBlob() {
        this.Version = null;
        this.Requirements = null;
        this.Constraints = null;
    }

    public PythonEnvironmentBlob setVersion(VersionEnvironmentBlob value) {
        this.Version = value;
        return this;
    }
    public PythonEnvironmentBlob setRequirements(List<PythonRequirementEnvironmentBlob> value) {
        this.Requirements = value;
        return this;
    }
    public PythonEnvironmentBlob setConstraints(List<PythonRequirementEnvironmentBlob> value) {
        this.Constraints = value;
        return this;
    }

    static public PythonEnvironmentBlob fromProto(ai.verta.modeldb.versioning.PythonEnvironmentBlob blob) {
        PythonEnvironmentBlob obj = new PythonEnvironmentBlob();
        {
            Function<ai.verta.modeldb.versioning.PythonEnvironmentBlob,VersionEnvironmentBlob> f = x -> { return VersionEnvironmentBlob.fromProto(x.getVersion()); };
            //VersionEnvironmentBlob.fromProto;
            if (f != null) {
                obj.Version = f.apply(blob);
            }
        }
        {
            Function<ai.verta.modeldb.versioning.PythonEnvironmentBlob,List<PythonRequirementEnvironmentBlob>> f = x -> { return ((Function<List<ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob>,List<PythonRequirementEnvironmentBlob>>) y -> y.stream().map(z -> PythonRequirementEnvironmentBlob.fromProto(z)).collect(Collectors.toList())).apply(x.getRequirementsList()); };
            //((Function<List<ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob>,List<PythonRequirementEnvironmentBlob>>) y -> y.stream().map(z -> PythonRequirementEnvironmentBlob.fromProto(z)).collect(Collectors.toList())).apply;
            if (f != null) {
                obj.Requirements = f.apply(blob);
            }
        }
        {
            Function<ai.verta.modeldb.versioning.PythonEnvironmentBlob,List<PythonRequirementEnvironmentBlob>> f = x -> { return ((Function<List<ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob>,List<PythonRequirementEnvironmentBlob>>) y -> y.stream().map(z -> PythonRequirementEnvironmentBlob.fromProto(z)).collect(Collectors.toList())).apply(x.getConstraintsList()); };
            //((Function<List<ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob>,List<PythonRequirementEnvironmentBlob>>) y -> y.stream().map(z -> PythonRequirementEnvironmentBlob.fromProto(z)).collect(Collectors.toList())).apply;
            if (f != null) {
                obj.Constraints = f.apply(blob);
            }
        }
        return obj;
    }

    public void preVisitShallow(Visitor visitor) throws ModelDBException {
        visitor.preVisitPythonEnvironmentBlob(this);
    }

    public void preVisitDeep(Visitor visitor) throws ModelDBException {
        this.preVisitShallow(visitor);
        visitor.preVisitDeepVersionEnvironmentBlob(this.Version);
        visitor.preVisitDeepListOfPythonRequirementEnvironmentBlob(this.Requirements);
        visitor.preVisitDeepListOfPythonRequirementEnvironmentBlob(this.Constraints);
    }

    public PythonEnvironmentBlob postVisitShallow(Visitor visitor) throws ModelDBException {
        return visitor.postVisitPythonEnvironmentBlob(this);
    }

    public PythonEnvironmentBlob postVisitDeep(Visitor visitor) throws ModelDBException {
        this.Version = visitor.postVisitDeepVersionEnvironmentBlob(this.Version);
        this.Requirements = visitor.postVisitDeepListOfPythonRequirementEnvironmentBlob(this.Requirements);
        this.Constraints = visitor.postVisitDeepListOfPythonRequirementEnvironmentBlob(this.Constraints);
        return this.postVisitShallow(visitor);
    }
}