#ifndef __EVENT_DATA_H__

#define __EVENT_DATA_H__

#pragma pack(1)
typedef struct
{
  //     0 0 BINARY(4) Length of watch information
  int watch_info_length;
  //     4 4 CHAR(7) Message watched
  char message_watched[7];
  //     11 B CHAR(1) Reserved
  char reserved0[1];
  //     12 C CHAR(10) Message queue name
  char message_queue_name[10];
  //     22 16 CHAR(10) Message queue library
  char message_queue_library[10];
  //     32 20 CHAR(10) Job name
  char job_name[10];
  //     42 2A CHAR(10) User name
  char user_name[10];
  //     52 34 CHAR(6) Job number
  char job_number[6];
  //     58 3A BINARY(4) Original replacement data length
  int original_replacement_data_length;
  //     62 3E CHAR(256) Sending program name
  char sending_program_name[256];
  //     318 13E CHAR(10) Sending module name
  char sending_module_name[10];
  //     328 148 BINARY(4) Offset to sending procedure name
  int offset_send_procedure_name;
  //     332 14C BINARY(4) Length of sending procedure name
  int length_send_procedure_name;
  //     336 150 CHAR(10) Receiving program name
  char receiving_program_name[10];
  //     346 15A CHAR(10) Receiving module name
  char receiving_module_name[10];
  //     356 164 BINARY(4) Offset to receiving procedure name
  int offset_rec_procedure_name;
  //     360 168 BINARY(4) Length of receiving procedure name
  int length_rec_procedure_name;
  //     364 16C BINARY(4) Message severity
  int message_severity;
  //     368 170 CHAR(10) Message type
  char message_type[10];
  //     378 17A CHAR(8) Message timestamp
  char message_timestamp[8];
  //     386 182 CHAR(4) Message key
  char message_key[4];
  //     390 186 CHAR(10) Message file name
  char message_file_name[10];
  //     400 190 CHAR(10) Message file library
  char message_file_library[10];
  //     410 19A CHAR(2) Reserved
  char reserved1[2];
  //     412 19C BINARY(4) Offset to comparison data
  int offset_comparison_data;
  //     416 1A0 BINARY(4) Length of comparison data
  int length_comparison_data;
  //     420 1A4 CHAR(10) Compare against
  char compare_against[10];
  //     430 1AE CHAR(2) Reserved
  char reserved2[2];
  //     432 1B0 BINARY(4) Comparison data CCSID
  int comparison_data_ccsid;
  //     436 1B4 BINARY(4) Offset where comparison data was found
  int offset_comparison_found;
  //     440 1B8 BINARY(4) Offset to replacement data
  int offset_replacement_data;
  //     444 1BC BINARY(4) Length of replacement data
  int length_replacement_data;
  //     448 1C0 BINARY(4) Replacement data CCSID
  int replacement_data_ccsid;
  //     452 1C4 CHAR(10) Sending user profile
  char sending_user_profile[10];
  //     462 1CE CHAR(10) Target job name
  char target_job_name[10];
  //     472 1D8 CHAR(10) Target job user name
  char target_job_user_name[10];
  //     482 1E2 CHAR(6) Target job number
  char target_job_number[6];
  //         **CHAR(*) Sending procedure name
  //         **CHAR(*) Receiving procedure name
  //         **CHAR(*) Message comparison data
  //         **CHAR(*) Message replacement data
} msg_event_raw;

typedef struct
{
  // Dec Hex 0 0 BINARY(4) Length of watch information
  int watch_info_length;
  // 4 4 CHAR(4) LIC Log major code
  char lic_log_major_code[4];
  // 8 8 CHAR(4) LIC Log minor code
  char lic_log_minor_code[4];
  // 12 C CHAR(8) LIC Log identifier
  char lic_log_identifier[8];
  // 20 14 CHAR(8) LIC Log timestamp
  char lic_log_timestamp[8];
  // 28 1C CHAR(8) TDE number
  char tde_number[8];
  // 36 24 CHAR(16) Task name
  char task_name[16];
  // 52 34 CHAR(30) Server type
  char server_type[30];
  // 82 52 CHAR(2) Exception ID
  char exception_id[2];
  // 84 54 CHAR(10) LIC job name
  char lic_job_name[10];
  // 94 5E CHAR(10) LIC user name
  char lic_user_name[10];
  // 104 68 CHAR(6) LIC job number
  char lic_job_number[6];
  // 110 6E CHAR(4) Reserved
  char reserved0[4];
  // 114 72 CHAR(8) Thread ID
  char thread_id[8];
  // 122 7A CHAR(8) LIC module compile timestamp
  char lic_module_compile_timestamp[8];
  // 130 82 CHAR(8) LIC module offset
  char lic_module_offset[8];
  // 138 8A CHAR(8) LIC module RU name
  char lic_module_ru_name[8];
  // 146 92 CHAR(48) LIC module name
  char lic_module_name[48];
  // 194 DA CHAR(128) LIC module entry point name
  char lic_module_entry_point_name[128];
  // 322 142 CHAR(1) LIC log compare against specified
  char lic_log_compare_against_specified[1];
  // 323 143 CHAR(1) Reserved
  char reserved1[1];
  // 324 144 BINARY(4) Offset to comparison data
  int offset_comparison_data;
  // 328 148 BINARY(4) Length of comparison data
  int length_comparison_data;
  // 332 14C CHAR(10) LIC log compare against
  char lic_log_compare_against[10];
  //     **CHAR(*) LIC log comparison data
  char comparison_data[1];
} vlog_event_raw;
#pragma pack(pop)

#endif