# hawkBit repository API

API for repository access. Contains:

- Entity Model
- Management service API

## Method naming concept (example target management)

Common CRUD methods are defined once in `RepositoryManagement<T, C, U>` (and `SoftDeletableRepositoryManagement` for soft
deletable entities); entity specific managements (e.g. `TargetManagement`) extend it and add entity specific methods.

Read method convention:

- `get...` - returns the entity (or list) directly and throws `EntityNotFoundException` if (at least one) not found
- `find...` - returns `Optional` (single) or skips not found (list / page), never throws `EntityNotFoundException`

```
// Count (RepositoryManagement)
long count();
long countByRsql(String rsql);
// Count by entity specific filter parameter (TargetManagement, example)
long countByRsqlAndCompatible(@NotEmpty String rsql, @NotNull Long distributionSetIdTypeId);

// Create entity
T create(@NotNull @Valid C create);
List<T> create(@NotNull Collection<@Valid C> create);

// Delete entities (throws EntityNotFoundException if one element does not exist (at least one not found in collection case))
void delete(long id);
void delete(@NotEmpty Collection<Long> ids);
void deleteByControllerId(@NotEmpty String controllerId); // TargetManagement

// Update (throws EntityNotFoundException if one element does not exist (at least one not found in collection case))
T update(@NotNull @Valid U update);
Map<Long, T> update(@NotNull Collection<@Valid U> update); // key is the entity ID

// Exist
boolean exists(long id);

// Read on technical ID
T get(long id);                                  // throws EntityNotFoundException
Optional<T> find(long id);                       // no EntityNotFoundException
List<T> get(@NotEmpty Collection<Long> ids);     // throws EntityNotFoundException if at least one not found
List<T> find(@NotEmpty Collection<Long> ids);    // skips not found

// Read on non-ID but unique constraint (TargetManagement)
Target getByControllerId(@NotEmpty String controllerId);                     // throws EntityNotFoundException
Optional<Target> findByControllerId(@NotEmpty String controllerId);          // no EntityNotFoundException
List<Target> findByControllerId(@NotEmpty Collection<String> controllerIDs); // skips not found

// Query/search repository (page might be empty, no EntityNotFoundException) (note: pageable is always last in signature)
Page<T> findAll(@NotNull Pageable pageable);
Page<T> findByRsql(@NotNull String rsql, @NotNull Pageable pageable);
Page<Target> findByAssignedDistributionSet(long distributionSetId, @NotNull Pageable pageable);
```
