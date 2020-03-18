package ai.verta.modeldb.versioning;

import static java.util.stream.Collectors.toMap;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.Blob;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.BlobDiff;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.DiffStatusEnumDiffStatus;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import ai.verta.modeldb.versioning.blob.diff.DiffComputer;
import ai.verta.modeldb.versioning.blob.diff.DiffMerger;
import ai.verta.modeldb.versioning.blob.diff.TypeChecker;
import ai.verta.modeldb.versioning.blob.factory.BlobFactory;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Status;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class BlobDAORdbImpl implements BlobDAO {

  private static final Logger LOGGER = LogManager.getLogger(BlobDAORdbImpl.class);

  public static final String TREE = "TREE";

  /**
   * Goes through each BlobExpanded creating TREE/BLOB node top down and computing SHA bottom up
   * there is a rootSHA which holds one TREE node of each BlobExpanded
   *
   * @throws ModelDBException
   */
  @Override
  public String setBlobs(List<BlobContainer> blobContainers, FileHasher fileHasher)
      throws NoSuchAlgorithmException, ModelDBException {
    TreeElem rootTree = new TreeElem();
    for (BlobContainer blobContainer : blobContainers) {
      // should save each blob during one session to avoid recurring entities ids
      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        session.beginTransaction();
        blobContainer.process(session, rootTree, fileHasher);
        session.getTransaction().commit();
      }
    }
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      final InternalFolderElement internalFolderElement = rootTree.saveFolders(session, fileHasher);
      session.getTransaction().commit();
      return internalFolderElement.getElementSha();
    }
  }

  private ai.verta.modeldb.versioning.Blob getBlob(
      Session session, InternalFolderElementEntity folderElementEntity) throws ModelDBException {
    return BlobFactory.create(folderElementEntity).getBlob(session);
  }

  private Folder getFolder(Session session, String commitSha, String folderSha) {
    Optional result =
        session
            .createQuery(
                "From "
                    + InternalFolderElementEntity.class.getSimpleName()
                    + " where folder_hash = '"
                    + folderSha
                    + "'")
            .list().stream()
            .map(
                d -> {
                  InternalFolderElementEntity entity = (InternalFolderElementEntity) d;
                  Folder.Builder folder = Folder.newBuilder();
                  FolderElement.Builder folderElement =
                      FolderElement.newBuilder()
                          .setElementName(entity.getElement_name())
                          .setCreatedByCommit(commitSha);

                  if (entity.getElement_type().equals(TREE)) {
                    folder.addSubFolders(folderElement);
                  } else {
                    folder.addBlobs(folderElement);
                  }
                  return folder.build();
                })
            .reduce((a, b) -> ((Folder) a).toBuilder().mergeFrom((Folder) b).build());

    if (result.isPresent()) {
      return (Folder) result.get();
    } else {
      return null;
    }
  }

  // TODO : check if there is a way to optimize on the calls to data base.
  // We should fetch data  in a single query.
  @Override
  public GetCommitComponentRequest.Response getCommitComponent(
      RepositoryFunction repositoryFunction, String commitHash, ProtocolStringList locationList)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = repositoryFunction.apply(session);
      CommitEntity commit = session.get(CommitEntity.class, commitHash);

      if (commit == null) {
        throw new ModelDBException("No such commit", Status.Code.NOT_FOUND);
      }

      if (!VersioningUtils.commitRepositoryMappingExists(session, commitHash, repository.getId())) {
        throw new ModelDBException("No such commit found in the repository", Status.Code.NOT_FOUND);
      }

      String folderHash = commit.getRootSha();
      if (locationList.isEmpty()) { // getting root
        Folder folder = getFolder(session, commit.getCommit_hash(), folderHash);
        session.getTransaction().commit();
        if (folder == null) { // root is empty
          return GetCommitComponentRequest.Response.newBuilder().build();
        }
        return GetCommitComponentRequest.Response.newBuilder().setFolder(folder).build();
      }
      for (int index = 0; index < locationList.size(); index++) {
        String folderLocation = locationList.get(index);
        String folderQueryHQL =
            "From "
                + InternalFolderElementEntity.class.getSimpleName()
                + " parentIfe WHERE parentIfe.element_name = :location AND parentIfe.folder_hash = :folderHash";
        Query<InternalFolderElementEntity> fetchTreeQuery = session.createQuery(folderQueryHQL);
        fetchTreeQuery.setParameter("location", folderLocation);
        fetchTreeQuery.setParameter("folderHash", folderHash);
        InternalFolderElementEntity elementEntity = fetchTreeQuery.uniqueResult();

        if (elementEntity == null) {
          LOGGER.warn(
              "No such folder found : {}. Failed at index {} looking for {}",
              folderLocation,
              index,
              folderLocation);
          throw new ModelDBException(
              "No such folder found : " + folderLocation, Status.Code.NOT_FOUND);
        }
        if (elementEntity.getElement_type().equals(TREE)) {
          folderHash = elementEntity.getElement_sha();
          if (index == locationList.size() - 1) {
            Folder folder = getFolder(session, commit.getCommit_hash(), folderHash);
            session.getTransaction().commit();
            if (folder == null) { // folder is empty
              return GetCommitComponentRequest.Response.newBuilder().build();
            }
            return GetCommitComponentRequest.Response.newBuilder().setFolder(folder).build();
          }
        } else {
          if (index == locationList.size() - 1) {
            ai.verta.modeldb.versioning.Blob blob = getBlob(session, elementEntity);
            session.getTransaction().commit();
            return GetCommitComponentRequest.Response.newBuilder().setBlob(blob).build();
          } else {
            throw new ModelDBException(
                "No such folder found : " + locationList.get(index + 1), Status.Code.NOT_FOUND);
          }
        }
      }
    } catch (Throwable throwable) {
      if (throwable instanceof ModelDBException) {
        throw (ModelDBException) throwable;
      }
      LOGGER.warn(throwable);
      throw new ModelDBException("Unknown error", Status.Code.INTERNAL);
    }
    throw new ModelDBException(
        "Unexpected logic issue found when fetching blobs", Status.Code.UNKNOWN);
  }

  /**
   * get the Folder Element pointed to by the parentFolderHash and elementName
   *
   * @param session
   * @param parentFolderHash : folder hash of the parent
   * @param elementName : element name of the element to be fetched
   * @return {@link List<InternalFolderElementEntity>}
   */
  private List<InternalFolderElementEntity> getFolderElement(
      Session session, String parentFolderHash, String elementName) {
    StringBuilder folderQueryHQLBuilder =
        new StringBuilder("From ")
            .append(InternalFolderElementEntity.class.getSimpleName())
            .append(" parentIfe WHERE parentIfe.folder_hash = :folderHash ");

    if (elementName != null && !elementName.isEmpty()) {
      folderQueryHQLBuilder.append("AND parentIfe.element_name = :elementName");
    }

    Query<InternalFolderElementEntity> fetchTreeQuery =
        session.createQuery(folderQueryHQLBuilder.toString());
    fetchTreeQuery.setParameter("folderHash", parentFolderHash);
    if (elementName != null && !elementName.isEmpty()) {
      fetchTreeQuery.setParameter("elementName", elementName);
    }
    return fetchTreeQuery.list();
  }

  boolean childContains(Set<?> list, Set<?> sublist) {
    return Collections.indexOfSubList(new LinkedList<>(list), new LinkedList<>(sublist)) != -1;
  }

  private Map<String, Map.Entry<BlobExpanded, String>> getChildFolderBlobMap(
      Session session,
      List<String> requestedLocation,
      Set<String> parentLocation,
      String parentFolderHash)
      throws ModelDBException {
    String folderQueryHQL =
        "From "
            + InternalFolderElementEntity.class.getSimpleName()
            + " parentIfe WHERE parentIfe.folder_hash = :folderHash";
    Query<InternalFolderElementEntity> fetchTreeQuery = session.createQuery(folderQueryHQL);
    fetchTreeQuery.setParameter("folderHash", parentFolderHash);
    List<InternalFolderElementEntity> childElementFolders = fetchTreeQuery.list();

    Map<String, Map.Entry<BlobExpanded, String>> childBlobExpandedMap = new LinkedHashMap<>();
    for (InternalFolderElementEntity childElementFolder : childElementFolders) {
      if (childElementFolder.getElement_type().equals(TREE)) {
        Set<String> childLocation = new LinkedHashSet<>(parentLocation);
        childLocation.add(childElementFolder.getElement_name());
        if (childContains(new LinkedHashSet<>(requestedLocation), childLocation)
            || childLocation.containsAll(requestedLocation)) {
          childBlobExpandedMap.putAll(
              getChildFolderBlobMap(
                  session, requestedLocation, childLocation, childElementFolder.getElement_sha()));
        }
      } else {
        if (parentLocation.containsAll(requestedLocation)) {
          ai.verta.modeldb.versioning.Blob blob = getBlob(session, childElementFolder);
          BlobExpanded blobExpanded =
              BlobExpanded.newBuilder()
                  .addAllLocation(parentLocation)
                  .addLocation(childElementFolder.getElement_name())
                  .setBlob(blob)
                  .build();
          childBlobExpandedMap.put(
              getStringFromLocationList(blobExpanded.getLocationList()),
              new AbstractMap.SimpleEntry<>(blobExpanded, childElementFolder.getElement_sha()));
        }
      }
    }
    return childBlobExpandedMap;
  }

  /**
   * Given a folderHash and a location list, collects all the blobs along the location list and
   * returns them with their location as set
   *
   * @param session
   * @param folderHash : the base folder to start the search for location list
   * @param locationList : list of trees and psossibly terminating with blob
   * @return
   * @throws ModelDBException
   */
  @Override
  public Map<String, BlobExpanded> getCommitBlobMap(
      Session session, String folderHash, List<String> locationList) throws ModelDBException {
    return convertToLocationBlobMap(getCommitBlobMapWithHash(session, folderHash, locationList));
  }

  private Map<String, BlobExpanded> convertToLocationBlobMap(
      Map<String, Map.Entry<BlobExpanded, String>> commitBlobMapWithHash) {
    return commitBlobMapWithHash.entrySet().stream()
        .collect(
            Collectors.toMap(
                Entry::getKey, stringEntryEntry -> stringEntryEntry.getValue().getKey()));
  }

  Map<String, Map.Entry<BlobExpanded, String>> getCommitBlobMapWithHash(
      Session session, String folderHash, List<String> locationList) throws ModelDBException {

    String parentLocation = locationList.size() == 0 ? null : locationList.get(0);
    List<InternalFolderElementEntity> parentFolderElementList =
        getFolderElement(session, folderHash, parentLocation);
    if (parentFolderElementList == null || parentFolderElementList.isEmpty()) {
      if (parentLocation
          != null) { // = null mainly is supporting the call on init commit which is an empty commit
        throw new ModelDBException(
            "No such folder found : " + parentLocation, Status.Code.NOT_FOUND);
      }
    }

    Map<String, Map.Entry<BlobExpanded, String>> finalLocationBlobMap = new LinkedHashMap<>();
    for (InternalFolderElementEntity parentFolderElement : parentFolderElementList) {
      if (!parentFolderElement.getElement_type().equals(TREE)) {
        ai.verta.modeldb.versioning.Blob blob = getBlob(session, parentFolderElement);
        BlobExpanded blobExpanded =
            BlobExpanded.newBuilder()
                .addLocation(parentFolderElement.getElement_name())
                .setBlob(blob)
                .build();
        finalLocationBlobMap.put(
            getStringFromLocationList(blobExpanded.getLocationList()),
            new SimpleEntry<>(blobExpanded, parentFolderElement.getElement_sha()));
      } else {
        // if this is tree, search further
        Set<String> location = new LinkedHashSet<>();
        Map<String, Map.Entry<BlobExpanded, String>> locationBlobList =
            getChildFolderBlobMap(session, locationList, location, folderHash);
        finalLocationBlobMap.putAll(locationBlobList);
      }
    }

    Comparator<Map.Entry<String, Map.Entry<BlobExpanded, String>>> locationComparator =
        Comparator.comparing(
            (Map.Entry<String, Map.Entry<BlobExpanded, String>> o) ->
                o.getKey().replaceAll("#", ""));

    finalLocationBlobMap =
        finalLocationBlobMap.entrySet().stream()
            .sorted(locationComparator)
            .collect(
                toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

    return finalLocationBlobMap;
  }

  @Override
  public ListCommitBlobsRequest.Response getCommitBlobsList(
      RepositoryFunction repositoryFunction, String commitHash, ProtocolStringList locationList)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();

      CommitEntity commit = session.get(CommitEntity.class, commitHash);
      if (commit == null) {
        throw new ModelDBException("No such commit", Status.Code.NOT_FOUND);
      }

      RepositoryEntity repository = repositoryFunction.apply(session);
      if (!VersioningUtils.commitRepositoryMappingExists(session, commitHash, repository.getId())) {
        throw new ModelDBException("No such commit found in the repository", Status.Code.NOT_FOUND);
      }
      Map<String, BlobExpanded> locationBlobMap =
          getCommitBlobMap(session, commit.getRootSha(), locationList);
      return ListCommitBlobsRequest.Response.newBuilder()
          .addAllBlobs(locationBlobMap.values())
          .build();
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      if (throwable instanceof ModelDBException) {
        throw (ModelDBException) throwable;
      }
      throw new ModelDBException("Unknown error", Status.Code.INTERNAL);
    }
  }

  @Override
  public ComputeRepositoryDiffRequest.Response computeRepositoryDiff(
      RepositoryFunction repositoryFunction, ComputeRepositoryDiffRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repositoryEntity = repositoryFunction.apply(session);

      CommitEntity internalCommitA = session.get(CommitEntity.class, request.getCommitA());
      if (internalCommitA == null) {
        throw new ModelDBException(
            "No such commit found : " + request.getCommitA(), Status.Code.NOT_FOUND);
      }

      CommitEntity internalCommitB = session.get(CommitEntity.class, request.getCommitB());
      if (internalCommitB == null) {
        throw new ModelDBException(
            "No such commit found : " + request.getCommitB(), Status.Code.NOT_FOUND);
      }

      if (!VersioningUtils.commitRepositoryMappingExists(
          session, internalCommitA.getCommit_hash(), repositoryEntity.getId())) {
        throw new ModelDBException(
            "No such commit found in the repository : " + internalCommitA.getCommit_hash(),
            Status.Code.NOT_FOUND);
      }

      if (!VersioningUtils.commitRepositoryMappingExists(
          session, internalCommitB.getCommit_hash(), repositoryEntity.getId())) {
        throw new ModelDBException(
            "No such commit found in the repository : " + internalCommitB.getCommit_hash(),
            Status.Code.NOT_FOUND);
      }
      // get list of blob expanded in both commit and group them in a map based on location
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobsMapCommitA =
          getCommitBlobMapWithHash(session, internalCommitA.getRootSha(), new ArrayList<>());

      Map<String, Map.Entry<BlobExpanded, String>> locationBlobsMapCommitB =
          getCommitBlobMapWithHash(session, internalCommitB.getRootSha(), new ArrayList<>());

      session.getTransaction().commit();

      // Added new blob location in the CommitB, locations in
      Set<String> addedLocations = new LinkedHashSet<>(locationBlobsMapCommitB.keySet());
      addedLocations.removeAll(locationBlobsMapCommitA.keySet());
      LOGGER.debug("Added location for Diff : {}", addedLocations);

      // deleted new blob location from the CommitA
      Set<String> deletedLocations = new LinkedHashSet<>(locationBlobsMapCommitA.keySet());
      deletedLocations.removeAll(locationBlobsMapCommitB.keySet());
      LOGGER.debug("Deleted location for Diff : {}", deletedLocations);

      // modified new blob location from the CommitA
      Set<String> modifiedLocations = new LinkedHashSet<>(locationBlobsMapCommitB.keySet());
      modifiedLocations.removeAll(addedLocations);
      Map<String, BlobExpanded> commonBlobs =
          locationBlobsMapCommitA.values().stream().collect(toMap(Entry::getValue, Entry::getKey));
      commonBlobs
          .keySet()
          .retainAll(
              locationBlobsMapCommitB.values().stream()
                  .map(Entry::getValue)
                  .collect(Collectors.toSet()));
      Map<String, BlobExpanded> locationBlobsCommon =
          getLocationWiseBlobExpandedMapFromCollection(commonBlobs.values());
      modifiedLocations.removeAll(locationBlobsCommon.keySet());
      LOGGER.debug("Modified location for Diff : {}", modifiedLocations);

      List<ai.verta.modeldb.versioning.BlobDiff> addedBlobDiffList =
          getAddedBlobDiff(addedLocations, convertToLocationBlobMap(locationBlobsMapCommitB));
      List<ai.verta.modeldb.versioning.BlobDiff> deletedBlobDiffList =
          getDeletedBlobDiff(deletedLocations, convertToLocationBlobMap(locationBlobsMapCommitA));
      List<ai.verta.modeldb.versioning.BlobDiff> modifiedBlobDiffList =
          getModifiedBlobDiff(
              modifiedLocations,
              convertToLocationBlobMap(locationBlobsMapCommitA),
              convertToLocationBlobMap(locationBlobsMapCommitB));

      return ComputeRepositoryDiffRequest.Response.newBuilder()
          .addAllDiffs(addedBlobDiffList)
          .addAllDiffs(deletedBlobDiffList)
          .addAllDiffs(modifiedBlobDiffList)
          .build();
    }
  }

  List<ai.verta.modeldb.versioning.BlobDiff> getAddedBlobDiff(
      Set<String> addedLocations, Map<String, BlobExpanded> locationBlobsMapCommitB) {
    return addedLocations.stream()
        .map(
            location -> {
              BlobExpanded blobExpanded = locationBlobsMapCommitB.get(location);
              BlobDiff diff = DiffComputer.computeBlobDiff(null, fromBlobProto(blobExpanded));
              diff.setStatus(DiffStatusEnumDiffStatus.fromProto(DiffStatus.ADDED));
              diff.setLocation(blobExpanded.getLocationList());
              return diff.toProto().build();
            })
        .collect(Collectors.toList());
  }

  private Blob fromBlobProto(BlobExpanded blobExpanded) {
    return Blob.fromProto(blobExpanded.getBlob());
  }

  List<ai.verta.modeldb.versioning.BlobDiff> getDeletedBlobDiff(
      Set<String> deletedLocations, Map<String, BlobExpanded> locationBlobsMapCommitA) {
    return deletedLocations.stream()
        .map(
            location -> {
              BlobExpanded blobExpanded = locationBlobsMapCommitA.get(location);

              BlobDiff diff = DiffComputer.computeBlobDiff(fromBlobProto(blobExpanded), null);
              diff.setStatus(DiffStatusEnumDiffStatus.fromProto(DiffStatus.DELETED));
              diff.setLocation(blobExpanded.getLocationList());
              return diff.toProto().build();
            })
        .collect(Collectors.toList());
  }

  List<ai.verta.modeldb.versioning.BlobDiff> getModifiedBlobDiff(
      Set<String> modifiedLocations,
      Map<String, BlobExpanded> locationBlobsMapCommitA,
      Map<String, BlobExpanded> locationBlobsMapCommitB) {
    return modifiedLocations.stream()
        .flatMap(
            location -> {
              BlobExpanded blobExpandedCommitA = locationBlobsMapCommitA.get(location);
              BlobExpanded blobExpandedCommitB = locationBlobsMapCommitB.get(location);
              final Blob a = fromBlobProto(blobExpandedCommitA);
              final Blob b = fromBlobProto(blobExpandedCommitB);
              if (TypeChecker.sameType(a, b)) {
                return Stream.of(
                    DiffComputer.computeBlobDiff(a, b)
                        .setLocation(blobExpandedCommitA.getLocationList())
                        .setStatus(DiffStatusEnumDiffStatus.fromProto(DiffStatus.MODIFIED))
                        .toProto()
                        .build());
              } else {
                return Stream.of(
                    DiffComputer.computeBlobDiff(a, null)
                        .setLocation(blobExpandedCommitA.getLocationList())
                        .setStatus(DiffStatusEnumDiffStatus.fromProto(DiffStatus.DELETED))
                        .toProto()
                        .build(),
                    DiffComputer.computeBlobDiff(null, b)
                        .setLocation(blobExpandedCommitB.getLocationList())
                        .setStatus(DiffStatusEnumDiffStatus.fromProto(DiffStatus.ADDED))
                        .toProto()
                        .build());
              }
            })
        .collect(Collectors.toList());
  }

  private Map<String, BlobExpanded> getLocationWiseBlobExpandedMapFromCollection(
      Collection<BlobExpanded> blobExpandeds) {
    return blobExpandeds.stream()
        .collect(
            Collectors.toMap(
                // TODO: Here used the `#` for joining the locations but if folder locations contain
                // TODO: - the `#` then this functionality will break.
                blobExpanded -> getStringFromLocationList(blobExpanded.getLocationList()),
                blobExpanded -> blobExpanded));
  }

  private String getStringFromLocationList(List<String> locationList) {
    return String.join("#", locationList);
  }

  @Override
  public List<BlobContainer> convertBlobDiffsToBlobs(
      CreateCommitRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repositoryEntity = repositoryFunction.apply(session);
      CommitEntity commitEntity = commitFunction.apply(session, session1 -> repositoryEntity);
      Map<String, BlobExpanded> locationBlobsMap =
          getCommitBlobMap(session, commitEntity.getRootSha(), new ArrayList<>());
      Map<String, BlobExpanded> locationBlobsMapNew = new LinkedHashMap<>();
      for (ai.verta.modeldb.versioning.BlobDiff blobDiff : request.getDiffsList()) {
        final ProtocolStringList locationList = blobDiff.getLocationList();
        if (locationList == null || locationList.isEmpty()) {
          throw new ModelDBException(
              "Location in BlobDiff should not be empty", Status.Code.INVALID_ARGUMENT);
        }
        BlobExpanded blobExpanded = locationBlobsMap.get(getStringFromLocationList(locationList));
        Blob blob =
            DiffMerger.mergeBlob(
                Blob.fromProto(blobExpanded.getBlob()), BlobDiff.fromProto(blobDiff));
        locationBlobsMapNew.put(
            getStringFromLocationList(blobExpanded.getLocationList()),
            BlobExpanded.newBuilder()
                .addAllLocation(blobExpanded.getLocationList())
                .setBlob(blob.toProto())
                .build());
      }
      locationBlobsMap.putAll(locationBlobsMapNew);
      List<BlobContainer> blobContainerList = new LinkedList<>();
      for (Map.Entry<String, BlobExpanded> blobExpandedEntry : locationBlobsMap.entrySet()) {
        blobContainerList.add(BlobContainer.create(blobExpandedEntry.getValue()));
      }
      return blobContainerList;
    }
  }
}