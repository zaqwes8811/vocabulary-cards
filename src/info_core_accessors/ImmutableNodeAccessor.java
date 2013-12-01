package info_core_accessors;

import com.google.common.collect.ImmutableList;

/**
 * Created with IntelliJ IDEA.
 * User: кей
 * Date: 21.07.13
 * Time: 12:51
 * To change this template use File | Settings | File Templates.
 */
public interface ImmutableNodeAccessor /*extends ContentHolder - no! лучше компазиция. */ {
  ImmutableList<Integer> getDistribution();

  // Для формирования пакета
  ImmutableList<String> getContent(Integer key);
  String getWord(Integer key);
}