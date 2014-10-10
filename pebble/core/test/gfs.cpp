#include <gtest/gtest.h>
#include <boost/bind.hpp>
#include "gfs.h"
#include "mock_pebble_accel.h"

class gfs_test : public testing::Test {
protected:
    static char* buffer;
    static int size;

    static void callback(char* b, int s) {
        buffer = b;
        size = s;
    }
};

char* gfs_test::buffer;
int gfs_test::size;

TEST_F(gfs_test, Trivial) {
    AccelRawData d[8];
    gfs_start(gfs_test::callback, 50);
    mock_accel_send_data(d, 8, 0);
    gfs_stop();
    EXPECT_EQ("234234234234234", gfs_test::buffer);
}
