#include "pebble.h"
#include "../core/main/gfs.h"
#include "../core/main/dl.h"

#define TIMER_MS 1000

static Window *window;

static GRect window_frame;

static Layer *disc_layer;

static AppTimer *timer;

static void disc_layer_update_callback(Layer *me, GContext *ctx) {
    graphics_context_set_text_color(ctx, GColorWhite);
    GRect bounds = layer_get_frame(me);

    char text[32];
    char wall_text[10];
    clock_copy_time_string(wall_text, 10);
    snprintf(text, 31, "%s Sent %d", wall_text, dl_count());

    graphics_draw_text(ctx,
            text,
            fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD),
            GRect(5, 5, bounds.size.w - 10, 100),
            GTextOverflowModeWordWrap,
            GTextAlignmentLeft,
            NULL);
}

static void timer_callback(void *data) {
    layer_mark_dirty(disc_layer);
    timer = app_timer_register(TIMER_MS, timer_callback, NULL);
}

static void window_load(Window *window) {
    Layer *window_layer = window_get_root_layer(window);
    GRect frame = window_frame = layer_get_frame(window_layer);

    disc_layer = layer_create(frame);
    layer_set_update_proc(disc_layer, disc_layer_update_callback);
    layer_add_child(window_layer, disc_layer);
}

static void window_unload(Window *window) {
    layer_destroy(disc_layer);
}

static void init(void) {
    window = window_create();
    window_set_window_handlers(window, (WindowHandlers) {
            .load = window_load,
            .unload = window_unload
    });
    window_stack_push(window, true /* Animated */);
    window_set_background_color(window, GColorBlack);

    gfs_start(dl_start(), 50);

    timer = app_timer_register(TIMER_MS, timer_callback, NULL);
}

static void deinit(void) {
    gfs_stop();
    dl_stop();

    window_destroy(window);
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}
