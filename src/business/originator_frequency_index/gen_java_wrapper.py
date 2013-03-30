# coding: utf-8
'''
Created on 30.03.2013

@author: кей
'''

import dals.os_io.io_wrapper as iow

import os

def _to_java_notation(sentence):
    result_string = ''
    space_was = False
    for at in sentence:
        if at != '_':
            if space_was:
                result_string += at.upper()
                space_was = False
            else:
                result_string += at
        else:
            space_was = True
    return result_string

def _to_java_notation_python_method_header(header):
    tmp = header.replace('def ', '(')
    save_method_name = tmp.split('(')[1]
    new_method_name = _to_java_notation(save_method_name)
    
    tmp = at.replace(save_method_name, new_method_name)
    return tmp 

def _get_public_class_headers(at):
    tmp = at.replace('class ','').split('(')[0]

    jheader = 'public interface I'+tmp+' {'
    pheader = 'class '+tmp+'(I'+tmp+'):'
    
    return jheader, pheader, 'I'+tmp


sets = iow.get_utf8_template()
sets['name'] = 'D:/github/content-translate-assistant/src/business/originator_frequency_index/orginator.py'
source_content = iow.file2list(sets)
result_java_iface_source = ['// Autogenerated']
result_python_iface_source = ['# Autogenerated']
header = ''
for at in source_content:
    # Выделяем открытые методы
    if 'def _' not in at and 'self' in at and 'def' in at:
            src_name = at[8:]
            src_name = src_name.replace(':','')
            splitted = src_name.split('#')
            src_name = splitted[1]+' '+splitted[0]+';'
            
            # In params
           
            src_name = src_name.replace('list_s_', 'List<String> ')
            src_name = src_name.replace('str_', 'String ')
            # returns
            
            src_name = src_name.replace('list', 'List')
            src_name = src_name.replace('string', 'String')
            src_name = src_name.replace('self, ', '')
            src_name = src_name.replace('self', '')
           
            src_name = _to_java_notation(src_name)
            result_java_iface_source.append('  '+src_name)
            method = _to_java_notation_python_method_header(at)
            result_python_iface_source.append(method)
            
    # Выделяем главный класс
    elif '#public' in at:
        j, p, header = _get_public_class_headers(at)
        result_java_iface_source.append(j)
        result_python_iface_source.append(p)
    else:
        result_python_iface_source.append(at)  
              
result_java_iface_source.append('}')

#for at in result_java_iface_source:
  #  print at
  
#result_python_iface_source.insert(2, 'import sys; sys.path.append())
result_python_iface_source.insert(3, 'import '+header)
sets['name'] = header[1:]+'.py'
sets['howOpen'] = 'w'
iow.list2file(sets, result_python_iface_source)

sets['name'] = header+'.java'
iow.list2file(sets, result_java_iface_source)


