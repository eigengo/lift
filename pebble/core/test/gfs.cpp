#include <gtest/gtest.h>
#include "gfs.h"

class gfs_test : public testing::Test {
};

TEST_F(gfs_test, Trivial) {
	gfs_start(NULL, 50);
	gfs_stop();
    EXPECT_EQ(1, 1);
}
