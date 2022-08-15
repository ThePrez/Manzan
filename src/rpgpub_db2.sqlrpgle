**free

Ctl-Opt NOMAIN;

Dcl-Proc db2_publish_message export;
  Dcl-Pi *N Int(10);
    _session_id Pointer Options(*String) Const;
    _msgid Pointer Options(*String) Const;
    _msg_type Pointer Options(*String) Const;
    _msg_severity Pointer Options(*String) Const;
    _msg_timestamp Pointer Options(*String) Const;
    _job Pointer Options(*String) Const;
    _sending_usrprf Pointer Options(*String) Const;
    _message Pointer Options(*String) Const;
    _sending_program_name Pointer Options(*String) Const;
    _sending_module_name Pointer Options(*String) Const;
    _sending_procedure_name Pointer Options(*String) Const;
  End-Pi;

  return 0;
End-Proc;

Dcl-Proc db2_publish_vlog export;
  Dcl-Pi *N Int(10);
    _session_id Pointer Options(*String) Const;
    _major_code Pointer Options(*String) Const;
    _minor_code Pointer Options(*String) Const;
    _log_id Pointer Options(*String) Const;
    _timestamp Pointer Options(*String) Const;
    _tde_number Pointer Options(*String) Const;
    _task_name Pointer Options(*String) Const;
    _server_type Pointer Options(*String) Const;
    _exception_id Pointer Options(*String) Const;
    _job Pointer Options(*String) Const;
    _thread_id Pointer Options(*String) Const;
    _module_offset Pointer Options(*String) Const;
    _module_ru_name Pointer Options(*String) Const;
    _module_name Pointer Options(*String) Const;
    _module_entry_point_name Pointer Options(*String) Const;
  End-Pi;

  return 0;
End-Proc;

Dcl-Proc db2_publish_pal export;
  Dcl-Pi *N Int(10);
    _session_id Pointer Options(*String) Const;
    _system_reference_code Pointer Options(*String) Const;
    _device_name Pointer Options(*String) Const;
    _device_type Pointer Options(*String) Const;
    _model Pointer Options(*String) Const;
    _serial_number Pointer Options(*String) Const;
    _resource_name Pointer Options(*String) Const;
    _log_identifier Pointer Options(*String) Const;
    _pal_timestamp Pointer Options(*String) Const;
    _reference_code Pointer Options(*String) Const;
    _secondary_code Pointer Options(*String) Const;
    _table_identifier Pointer Options(*String) Const;
    _sequence int(10) const;
  End-Pi;

  return 0;
End-Proc;

Dcl-Proc db2_publish_other export;
  Dcl-Pi *N Int(10);
    _session_id Pointer Options(*String) Const;
    _event_type Pointer Options(*String) Const;
  End-Pi;
End-Proc;