load("@rules_cc//cc:defs.bzl", "cc_test")

cc_test(
    name = "test_util",
    size = "small",
    srcs = ["test/ile/test_util.cc", "ile/src/manzan.h"],
    deps = [
        "@googletest//:gtest",
        "@googletest//:gtest_main",
    ],
    includes = ["ile/src"],
    copts = [
        "-Iile/src",
        "-std=c++17",  # GoogleTest 1.17.0+ requires C++17
    ],
    # Disable default compile flags to prevent C++14 from being added
    features = ["-default_compile_flags"],
)