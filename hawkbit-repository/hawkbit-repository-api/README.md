# hawkBit repository API

API for repository access. Contains:

- Entity Model
- Management service API

## Method naming concept (example target management)

```
// Count all targets
Long count()

// Count by filter parameter (example)
Long countByTargetFilterQuery(@NotEmpty String targetFilterQuery);

//Create entity
List<Target> create(@NotEmpty Collection<TargetCreate> create)
Target create(@NotNull TargetCreate create)

//Delete entities (throws EntityNotFoundException if one element does not exist (at least one not found in collection case))
void delete(@NotEmpty Collection<Long> targetIDs);
void delete(@NotNull Long targetID);
void deleteByControllerId(@NotEmpty String controllerId);
void deleteByControllerId(@NotEmpty Collection<String> controllerId);

//Update Target (throws EntityNotFoundException if one element does not exist (at least one not found in collection case))
List<Target> update(@NotEmpty Collection<TargetUpdate> update);
Target update(@NotNull TargetUpdate update);

//Exist
boolean exists(@NotNull Long targetId)
boolean existsByAssignedDistributionSet(@NotNull Long distributionSetID);

// Read methods
// Find one on technical ID (Optional, no EntityNotFoundException)
Optional<Target> get(@NotNull Long targetId);
List<Target> get(@NotEmpty Collection<Long> targetId);

// Find one on non-ID but unique constraint (Optional, no EntityNotFoundException)
Optional<Target> getByControllerID(@NotEmpty String controllerId);
List<Target> getByControllerID(@NotEmpty Collection<String> controllerId);

// Find one on non-ID but and non unique constraint (Optional, no EntityNotFoundException)
Optional<Target> findFirstByDescription(@NotEmpty String description);

// Query/search repository (page might be empty, no EntityNotFoundException) (note: pageReq always first in signature)
Page<Target> findByAssignedDistributionSet(@NotNull Pageable pageReq, @NotNull Long distributionSetID);

```