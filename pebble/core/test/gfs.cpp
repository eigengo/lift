#include <gtest/gtest.h>

class gfs_test : public testing::Test {
};

TEST_F(gfs_test, Trivial) {
    EXPECT_EQ(1, 1);
}
