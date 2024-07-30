#include <gtest/gtest.h>
#include "manzan.h"

// Demonstrate some basic assertions.
TEST(UTILITY, testCreateTrimmedString) {
  std::string spaceAtEnd = "hello     ";
  std::string trimmed0 = createTrimmedString(spaceAtEnd, sizeof(spaceAtEnd));
  EXPECT_STREQ(trimmed0.c_str(), "hello");

  std::string spaceInMiddle = "hello     d";
  std::string trimmed1 = createTrimmedString(spaceInMiddle, sizeof(spaceInMiddle));
  EXPECT_STREQ(trimmed1.c_str(), "hello     d");

  std::string spaceAtBeginning = "    hello";
  std::string trimmed2 = createTrimmedString(spaceAtBeginning, sizeof(spaceAtBeginning));
  EXPECT_STREQ(trimmed2.c_str(), "    hello");

  std::string allSpace = "    ";
  std::string trimmed3 = createTrimmedString(allSpace, sizeof(allSpace));
  EXPECT_STREQ(trimmed3.c_str(), "dd");

  
}
