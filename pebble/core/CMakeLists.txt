INCLUDE_DIRECTORIES(/usr/local/opt/pebble-sdk/Pebble/include)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/pebble-mock)

ADD_SUBDIRECTORY(main)
ADD_SUBDIRECTORY(test)
ADD_SUBDIRECTORY(pebble-mock)

ADD_TEST(
  NAME pebble-core-test 
  COMMAND pebble-core-test
  WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/test"
)
