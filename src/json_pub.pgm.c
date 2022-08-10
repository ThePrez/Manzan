#include <stdio.h>
#include <fcntl.h>
#include <qp0ztrc.h>
#include <qmhrtvm.h>
#include <qsnddtaq.h>
#include <qtqiconv.h>

int to_utf8(char *out, size_t out_len, const char *in)
{
  QtqCode_T tocode;
  memset(&tocode, 0, sizeof(tocode));
  tocode.CCSID = 819;
  QtqCode_T fromcode;
  memset(&fromcode, 0, sizeof(fromcode));
  iconv_t cd = QtqIconvOpen(&tocode, &fromcode);
  // if ((iconv_t)-1 == (iconv_t)cd)
  // {
  //   Qp0zLprintf("Error in opening conversion descriptors\n");
  //   return 8;
  // }

  size_t inleft = strlen(in);
  size_t outleft = out_len;
  char *input = (char *)in;
  char *output = out;

  int rc = iconv(cd, &input, &inleft, &output, &outleft);
  if (rc == -1)
  {
    Qp0zLprintf("Error in converting characters\n");
    return 9;
  }
  return iconv_close(cd);
}
typedef struct parms {
  char msg_id[7];
  char n1[1];
  char job_no[6];
  char n2[1];
  char job_user[10];
  char n3[1];
  char job_name[10];
  char n4[1];
  char job_number[6];
  char n5[1];
  char reserved1[4];
  char n6[1];
  char send_program_name[256];
  char n7[1];
  char send_module_name[10];
  char n8[1];
  int offset_send_procedure_name;
  int offset_send_procedure_length;
  char receiving_program_name[10];
  char n9[1];
  char receiving_module_name[10];
  char n10[1];
  int offset_rec_procedure_name;
  int offset_rec_procedure_length;
  int message_sev;
  char message_type[10];
  char n11[1];
  char message_ts[10];
  char n12[1];
  char message_key[4];
  char n13[1];
  char message_file_obj[10];
  char n14[1];
  char message_file_lib[10];
  char n15[1];
  char reserved[2];
  char n16[1];
  // From here
  int comparison_offset;
  int comparison_length;
  char compare_again[10];
  char reserved2[2];
  char n17[1];
  int comparison_ccsid;
  int offset_comparison_found;
  int offset_replacement_data;
  int replacement_data_length;
  int replacement_ccsid;
  // To here, not really used.
  char sending_user[10];
  char n18[1];
  char target_job_name[10];
  char n19[1];
  char target_user_name[10];
  char n20[1];
  char target_job_number[6];
  char n21[1];
  char sending_procedure_name[256];
  char n22[1];
  char receiving_procedure_name[256];
  char n23[1];
  char message_comparison_data[72];
  char n24[1];
  char message_replacement_data[512];
  char n25[1];
} liam;

int main(int argc, char *argv[])
{
  Qp0zLprintf("woop29\n");
  liam parms;
  memset(&parms, 0, sizeof(parms));
  strncpy(parms.msg_id, argv[1], sizeof(parms.msg_id));
  strncpy(parms.job_no, argv[2], sizeof(parms.job_no));
  strncpy(parms.job_user, argv[3], sizeof(parms.job_user));
  strncpy(parms.job_name, argv[4], sizeof(parms.job_name));
  strncpy(parms.message_file_lib, argv[5], sizeof(parms.message_file_lib));
  strncpy(parms.message_file_obj, argv[6], sizeof(parms.message_file_obj));
  strncpy(parms.message_replacement_data, argv[7], sizeof(parms.message_replacement_data));


  FILE* fd = fopen("/home/LINUX/manzantest", "w");
  fprintf(fd, "wooopity woop\n");
  printf("FILE* is %p\n", fd);
  fprintf(fd, "msg_id: %s\n", parms.msg_id);
  fprintf(fd, "job: %s/%s/%s\n", parms.job_no, parms.job_name, parms.job_user);
  fprintf(fd, "replacement data: %s\n", parms.message_replacement_data);


  char buffity_buf[4444];
  sprintf(buffity_buf, "{\n\"msg_id\": \"%s\",\n\"job\": \"%s/%s/%s\",\n\"replacement_data\": \"%s\"\n}\n", parms.msg_id, parms.job_no, parms.job_name, parms.job_user, parms.message_replacement_data);
  char buffity_buf_utf8[4455];
  to_utf8(buffity_buf_utf8, sizeof(buffity_buf_utf8), buffity_buf);
  QSNDDTAQ("MANZANDTAQ", "JESSEG    ", 1+strlen(buffity_buf_utf8), buffity_buf_utf8);

  fflush(fd);
  fclose(fd);
  return 0;

  // memset(json, 0x00, sizeof(json));
  // sprintf(json, "{\n\"msg_id\": \"%s\",\n\"job\": \"%s/%s/%s\",\n\"message\": \"%s\"\n}\n", msgid,
  //         job_name,
  //         user_name,
  //         job_number,
  //         msg_info_buf.message);
  // fprintf(fd, "JSON is '%s'\n", json);
  // char buffity_buf[4444];
  // char buffity_buf_utf8[4455];
  // to_utf8(buffity_buf_utf8, sizeof(buffity_buf_utf8), json);
  // QSNDDTAQ("MANZANDTAQ", "JESSEG    ", 1 + strlen(buffity_buf_utf8), buffity_buf_utf8);
}