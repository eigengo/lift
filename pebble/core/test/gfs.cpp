#include <gtest/gtest.h>
#include "gfs.h"
#include "mock.h"

using namespace pebble::mock;

class gfs_test : public testing::Test {
protected:
    static uint8_t* buffer;
    static int size;

    static void callback(uint8_t* b, int s) {
        buffer = b;
        size = s;
    }
};

uint8_t* gfs_test::buffer;
int gfs_test::size;

TEST_F(gfs_test, Version1) {
    // we have 12000B for our buffer, which means space for 2400 samples
    // the accelerometer is configured to receive multiple of 8 number
    // if AccelRawData values.

    // that is, we must push 300 * 8 packets of accel data
    std::vector<AccelRawData> mock_data;
    AccelRawData a = { .x = 1, .y = 100, .z = -400 };
    for (int i = 0; i < 8; i++) mock_data.push_back(a);

    gfs_start(gfs_test::callback, 50);
    for (int i = 0; i < 300; i++) Pebble::accelService << mock_data;

    gfs_stop();

    gfs_header *h = reinterpret_cast<gfs_header*>(gfs_test::buffer);
    EXPECT_EQ(h->h1, GFS_HEADER_H1);
    EXPECT_EQ(h->h2, GFS_HEADER_H2);
    EXPECT_EQ(h->size, GFS_BUFFER_SIZE);

    gfs_packed_accel_data *data = reinterpret_cast<gfs_packed_accel_data*>(gfs_test::buffer + sizeof(gfs_header));
    for (int i = 0; i < h->size; i++) {
        EXPECT_EQ(data[i].x_val, mock_data[i].x);
        EXPECT_EQ(data[i].y_val, mock_data[i].y);
        EXPECT_EQ(data[i].z_val, mock_data[i].z);
    }
}

