// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;

public class HyperparameterSetConfigBlob {
    public String Name;
    public ContinuousHyperparameterSetConfigBlob Continuous;
    public DiscreteHyperparameterSetConfigBlob Discrete;

    public HyperparameterSetConfigBlob() {
        this.Name = null;
        this.Continuous = null;
        this.Discrete = null;
    }

    public HyperparameterSetConfigBlob setName(String value) {
        this.Name = value;
        return this;
    }
    public HyperparameterSetConfigBlob setContinuous(ContinuousHyperparameterSetConfigBlob value) {
        this.Continuous = value;
        return this;
    }
    public HyperparameterSetConfigBlob setDiscrete(DiscreteHyperparameterSetConfigBlob value) {
        this.Discrete = value;
        return this;
    }

    static public HyperparameterSetConfigBlob fromProto(ai.verta.modeldb.versioning.HyperparameterSetConfigBlob blob) {
        HyperparameterSetConfigBlob obj = new HyperparameterSetConfigBlob();
        {
            Function<ai.verta.modeldb.versioning.HyperparameterSetConfigBlob,String> f = x -> { return (x.getName()); };
            //;
            if (f != null) {
                obj.Name = f.apply(blob);
            }
        }
        {
            Function<ai.verta.modeldb.versioning.HyperparameterSetConfigBlob,ContinuousHyperparameterSetConfigBlob> f = x -> { return ContinuousHyperparameterSetConfigBlob.fromProto(x.getContinuous()); };
            //ContinuousHyperparameterSetConfigBlob.fromProto;
            if (f != null) {
                obj.Continuous = f.apply(blob);
            }
        }
        {
            Function<ai.verta.modeldb.versioning.HyperparameterSetConfigBlob,DiscreteHyperparameterSetConfigBlob> f = x -> { return DiscreteHyperparameterSetConfigBlob.fromProto(x.getDiscrete()); };
            //DiscreteHyperparameterSetConfigBlob.fromProto;
            if (f != null) {
                obj.Discrete = f.apply(blob);
            }
        }
        return obj;
    }

    public void preVisitShallow(Visitor visitor) throws ModelDBException {
        visitor.preVisitHyperparameterSetConfigBlob(this);
    }

    public void preVisitDeep(Visitor visitor) throws ModelDBException {
        this.preVisitShallow(visitor);
        visitor.preVisitDeepString(this.Name);
        visitor.preVisitDeepContinuousHyperparameterSetConfigBlob(this.Continuous);
        visitor.preVisitDeepDiscreteHyperparameterSetConfigBlob(this.Discrete);
    }

    public HyperparameterSetConfigBlob postVisitShallow(Visitor visitor) throws ModelDBException {
        return visitor.postVisitHyperparameterSetConfigBlob(this);
    }

    public HyperparameterSetConfigBlob postVisitDeep(Visitor visitor) throws ModelDBException {
        this.Name = visitor.postVisitDeepString(this.Name);
        this.Continuous = visitor.postVisitDeepContinuousHyperparameterSetConfigBlob(this.Continuous);
        this.Discrete = visitor.postVisitDeepDiscreteHyperparameterSetConfigBlob(this.Discrete);
        return this.postVisitShallow(visitor);
    }
}