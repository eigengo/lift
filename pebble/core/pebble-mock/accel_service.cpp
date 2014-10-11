#include "accel_service.h"
#include "mock.h"

using namespace pebble::mock;

AccelService& AccelService::operator<<(const AccelRawData data) {
    this->raw_data.push_back(data);
    trigger_raw();
    return *this;
}

AccelService& AccelService::operator<<(const std::vector <AccelRawData> &data) {
    this->raw_data.insert(this->raw_data.end(), data.begin(), data.end());
    trigger_raw();
    return *this;
}

void AccelService::trigger_raw() {
    for (int i = 0; i < this->raw_data.size() / this->samples_per_update; ++i) {
        std::vector<AccelRawData> update(this->raw_data.begin() + i * this->samples_per_update,
                                         this->raw_data.begin() + i * this->samples_per_update + this->samples_per_update);
        this->raw_data_handler(update.data(), (uint32_t) update.size(), 0);
    }

    this->raw_data.clear();
}

void AccelService::accel_raw_data_service_subscribe(uint32_t samples_per_update, AccelRawDataHandler handler) {
    if (samples_per_update < 1) throw std::runtime_error("");
    if (handler == nullptr) throw std::runtime_error("");

    this->raw_data_handler = handler;
    this->samples_per_update = samples_per_update;
}

extern "C" {

void accel_raw_data_service_subscribe(uint32_t samples_per_update, AccelRawDataHandler handler) {
    Pebble::accelService.accel_raw_data_service_subscribe(samples_per_update, handler);
}

}

