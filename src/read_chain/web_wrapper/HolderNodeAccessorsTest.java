package read_chain.web_wrapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import info_core_accessors.*;
import org.junit.Test;
import through_functional.configurator.GlobalConfigurator;

public class HolderNodeAccessorsTest {
  @Test
  public void testGeneratePackage() throws Exception {
    Wrapper wrapper = new Wrapper();
    String pathToCfgFile = "./my.yaml";
    ImmutableSet<String> namesNodes = new GlobalConfigurator(pathToCfgFile).getRegisteredNodes().get();
    ImmutableList<ImmutableNodeAccessor> accessors = wrapper.getNodes(
      namesNodes, new FabricImmutableNodeAccessors());
    HolderNodeAccessors container = new HolderNodeAccessors(accessors);
    container.getPerWordData(0);
  }
}