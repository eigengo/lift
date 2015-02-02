#include <gtest/gtest.h>
#include "gfs.h"
#include "mock.h"

using namespace pebble::mock;

class gfs_test : public testing::Test {
protected:
    static uint8_t *buffer;
    static uint16_t size;
public:
    static void gfs_callback(uint8_t *b, uint16_t s);

    virtual ~gfs_test();
};

gfs_test::~gfs_test() {
   if (buffer != nullptr) free(buffer);
}

void gfs_test::gfs_callback(uint8_t *b, uint16_t s) {
    if (buffer != nullptr) free(buffer);
    buffer = (uint8_t *)malloc(s);
    memcpy(buffer, b, s);
    size = s;
}

uint8_t *gfs_test::buffer;
uint16_t gfs_test::size;

TEST_F(gfs_test, Version1) {
    // we have 12000B for our buffer, which means space for 2384 samples
    // the accelerometer is configured to receive multiple of 8 number
    // if AccelRawData values.

    // that is, we must push 376 * 8 packets of accel data
    std::vector<AccelRawData> mock_data;
    AccelRawData a = { .x = 1000, .y = 5000, .z = -5000 };
    for (int i = 0; i < 16; i++) mock_data.push_back(a);

    gfs_start(gfs_test::gfs_callback, GFS_SAMPLING_100HZ);
    for (int i = 0; i < 126; i++) Pebble::accelService << mock_data;

    ASSERT_TRUE(gfs_test::buffer != nullptr);
    gfs_header *h = reinterpret_cast<gfs_header*>(gfs_test::buffer);
    EXPECT_EQ(h->type, GFS_HEADER_TYPE);
    EXPECT_EQ(h->count, 124);
    EXPECT_EQ(h->samples_per_second, GFS_SAMPLING_100HZ);

    gfs_packed_accel_data *data = reinterpret_cast<gfs_packed_accel_data*>(gfs_test::buffer + sizeof(gfs_header));
    for (int i = 0; i < h->count; i++) {
        EXPECT_EQ(data[i].x_val, 1000);
        EXPECT_EQ(data[i].y_val, 4095);
        EXPECT_EQ(data[i].z_val, -4095);
    }

    gfs_stop();

}

