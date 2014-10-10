INCLUDE_DIRECTORIES(/usr/local/Cellar/pebble-sdk/2.5/Pebble/include)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/pebble-include)

ADD_SUBDIRECTORY(main)
ADD_SUBDIRECTORY(test)

ADD_TEST(
  NAME pebble-core-test 
  COMMAND pebble-core-test
  WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/test"
)
