#include <gtest/gtest.h>
#include "gfs.h"
#include "mock.h"

using namespace pebble::mock;

class gfs_test : public testing::Test {
protected:
    static uint8_t *buffer;
    static uint16_t size;
    static uint16_t count;
public:
    static void gfs_callback(uint8_t *b, uint16_t s, uint16_t c);
};

void gfs_test::gfs_callback(uint8_t *b, uint16_t s, uint16_t c) {
    buffer = b;
    size = s;
    count = c;
}

uint8_t *gfs_test::buffer;
uint16_t gfs_test::size;
uint16_t gfs_test::count;

TEST_F(gfs_test, Version1) {
    // we have 12000B for our buffer, which means space for 2400 samples
    // the accelerometer is configured to receive multiple of 8 number
    // if AccelRawData values.

    // that is, we must push 376 * 8 packets of accel data
    std::vector<AccelRawData> mock_data;
    AccelRawData a = { .x = 1, .y = 100, .z = -400 };
    for (int i = 0; i < 8; i++) mock_data.push_back(a);

    gfs_start(gfs_test::gfs_callback, 50);
    for (int i = 0; i < 376; i++) Pebble::accelService << mock_data;

    ASSERT_TRUE(gfs_test::buffer != nullptr);
    gfs_header *h = reinterpret_cast<gfs_header*>(gfs_test::buffer);
    EXPECT_EQ(h->h1, GFS_HEADER_H1);
    EXPECT_EQ(h->h2, GFS_HEADER_H2);

    gfs_packed_accel_data *data = reinterpret_cast<gfs_packed_accel_data*>(gfs_test::buffer + sizeof(gfs_header));

    for (int i = 0; i < gfs_test::count; i++) {
        EXPECT_EQ(data[i].x_val, 1);
        EXPECT_EQ(data[i].y_val, 100);
        EXPECT_EQ(data[i].z_val, -400);
    }

    gfs_stop();

}

