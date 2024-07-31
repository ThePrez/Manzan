#include <gtest/gtest.h>
#include "manzan.h"

// Demonstrate some basic assertions.
TEST(UTILITY, testBUFSTRN) {

    std::string spaceAtEnd = "hello     ";
    BUFSTRN(trimmed0, spaceAtEnd, sizeof(spaceAtEnd));
    EXPECT_STREQ(trimmed0.c_str(), "hello");
 

    std::string spaceInMiddle = "hello     d";
    BUFSTRN(trimmed1, spaceInMiddle, sizeof(spaceInMiddle));
    EXPECT_STREQ(trimmed1.c_str(), "hello     d");

    std::string spaceAtBeginning = "    hello";
    BUFSTRN(trimmed2, spaceAtBeginning, sizeof(spaceAtBeginning));
    EXPECT_STREQ(trimmed2.c_str(), "    hello");

    std::string allSpace = "    ";
    BUFSTRN(trimmed3, allSpace, sizeof(allSpace));
    EXPECT_STREQ(trimmed3.c_str(), "");

    std::string emptyString = "";
    BUFSTRN(trimmed4, emptyString, sizeof(emptyString));
    EXPECT_STREQ(trimmed4.c_str(), "");

}
