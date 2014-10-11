#include <gtest/gtest.h>
#include "gfs.h"
#include "mock.h"

using namespace pebble::mock;

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
    std::vector<AccelRawData> d;
    AccelRawData a = {.x = 1, .y = 100, .z = -400 };
    for (int i = 0; i < 20; i++) d.push_back(a);

    gfs_start(gfs_test::callback, 50);
    Pebble::accelService << d;
    gfs_stop();
    EXPECT_EQ("234234234234234", gfs_test::buffer);
}

