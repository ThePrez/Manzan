load("@rules_cc//cc:defs.bzl", "cc_test")

cc_test(
    name = "test_util",
    size = "small",
    srcs = ["test/ile/test_util.cc", "ile/src/manzan.h"],
    deps = [
        "@googletest//:gtest",
        "@googletest//:gtest_main",
    ],
    includes = ["ile/src"],  # Adjusted to relative path
    copts = ["-Iile/src"],
)