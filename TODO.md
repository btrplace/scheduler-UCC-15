# TO DO

## Btrplace
- [X] Create a new objective: Minimizing energy consumption
- [X] Custom search strategies for MinEnergy objective
- [X] Simplify the creation of static routes(done from input XML file)
- [X] Convert/Import g5k topology description files to static routes
  - [X] Import G5k XML topology description files
  - [P] Manage AS routes (inter-sites routing)
- [ ] Export networkView to XML
- [ ] Define an evaluation protocol
  - [ ] Flexibility
  - [ ] Scale
  - [ ] Performance (Define metrics)
  - [ ] Efficiency
- [ ] Create IaaS actuators
  - [ ] G5k
  - [P] OpenStack
  - [P] OpenNebula

## Btrpsl
- [X] Implement constraints in Btrpsl
  - [X] Sync
  - [X] Serialize
  - [X] Before
  - [X] MaxWatts

### DC4Cities
- [P] Adapt the PowerBudget
- [P] Convert a solution to an activity option

### Packaging
- [ ] Auto add Network and Energy views with default values (minimal impact)
