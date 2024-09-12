#include <gtest/gtest.h>
#include "manzan.h"

// Demonstrate some basic assertions.
TEST(UTILITY, testBUFSTRN) {

    char spaceAtEnd[] = "hello     ";
    BUFSTRN(trimmed0, spaceAtEnd, strlen(spaceAtEnd));
    EXPECT_STREQ(trimmed0.c_str(), "hello");
 
    char spaceInMiddle[] = "hello     d";
    BUFSTRN(trimmed1, spaceInMiddle, strlen(spaceInMiddle));
    EXPECT_STREQ(trimmed1.c_str(), "hello     d");

    char spaceAtBeginning[] = "    hello";
    BUFSTRN(trimmed2, spaceAtBeginning, strlen(spaceAtBeginning));
    EXPECT_STREQ(trimmed2.c_str(), "    hello");

    char allSpace[] = "    ";
    BUFSTRN(trimmed3, allSpace, strlen(allSpace));
    EXPECT_STREQ(trimmed3.c_str(), "");

    char emptyString[] = "";
    BUFSTRN(trimmed4, emptyString, strlen(emptyString));
    EXPECT_STREQ(trimmed4.c_str(), "");

}
