INCLUDE_DIRECTORIES(/usr/local/opt/pebble-sdk/Pebble/include)

ADD_SUBDIRECTORY(main)
ADD_SUBDIRECTORY(test)

ADD_TEST(
  NAME pebble-core-test 
  COMMAND pebble-core-test
  WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/test"
)
