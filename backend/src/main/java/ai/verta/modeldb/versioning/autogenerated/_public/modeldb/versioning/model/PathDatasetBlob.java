// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;

public class PathDatasetBlob {
    public List<PathDatasetComponentBlob> Components;

    public PathDatasetBlob() {
        this.Components = null;
    }

    public PathDatasetBlob setComponents(List<PathDatasetComponentBlob> value) {
        this.Components = value;
        return this;
    }

    static public PathDatasetBlob fromProto(ai.verta.modeldb.versioning.PathDatasetBlob blob) {
        PathDatasetBlob obj = new PathDatasetBlob();
        {
            Function<ai.verta.modeldb.versioning.PathDatasetBlob,List<PathDatasetComponentBlob>> f = x -> { return ((Function<List<ai.verta.modeldb.versioning.PathDatasetComponentBlob>,List<PathDatasetComponentBlob>>) y -> y.stream().map(z -> PathDatasetComponentBlob.fromProto(z)).collect(Collectors.toList())).apply(x.getComponentsList()); };
            //((Function<List<ai.verta.modeldb.versioning.PathDatasetComponentBlob>,List<PathDatasetComponentBlob>>) y -> y.stream().map(z -> PathDatasetComponentBlob.fromProto(z)).collect(Collectors.toList())).apply;
            if (f != null) {
                obj.Components = f.apply(blob);
            }
        }
        return obj;
    }

    public void preVisitShallow(Visitor visitor) throws ModelDBException {
        visitor.preVisitPathDatasetBlob(this);
    }

    public void preVisitDeep(Visitor visitor) throws ModelDBException {
        this.preVisitShallow(visitor);
        visitor.preVisitDeepListOfPathDatasetComponentBlob(this.Components);
    }

    public PathDatasetBlob postVisitShallow(Visitor visitor) throws ModelDBException {
        return visitor.postVisitPathDatasetBlob(this);
    }

    public PathDatasetBlob postVisitDeep(Visitor visitor) throws ModelDBException {
        this.Components = visitor.postVisitDeepListOfPathDatasetComponentBlob(this.Components);
        return this.postVisitShallow(visitor);
    }
}
