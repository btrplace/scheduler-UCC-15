# TO DO

## Btrplace
- [X] Create a new objective: Minimizing energy consumption
- [X] Custom search strategies for MinEnergy objective
- [X] Simplify the creation of static routes(done from input XML file)
- [X] Convert/Import g5k topology description files to static routes
  - [X] Import G5k XML topology description files
  - [X] Manage AS routes (inter-sites routing)
- [ ] Create a new btrplace-g5k module which will act as an actuator for g5k experiments
- [ ] Define an evaluation protocol
  - [ ] Flexibility
  - [ ] Scale
  - [ ] Performance (Define metrics)
  - [ ] Efficiency
- [ ] Patch/Improve topo5k and update SimgridModelBuilder accordingly
- [P] Export networkView to XML
- [P] Create IaaS actuators
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
