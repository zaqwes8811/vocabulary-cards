package pipeline.math;

import java.io.Serializable;

//http://stackoverflow.com/questions/5560176/is-integer-immutable
// сохраняется в базе!
public class DistributionElement implements Comparable<DistributionElement>, Serializable {
  private static final long serialVersionUID = -3004460083142639254L;
  
	// FIXME: make get/set
	public Integer frequency;
  public Boolean enabled = true;
  public Boolean inBoundary = false;
  
  public void setBoundary(Boolean value) {
  	inBoundary = value;
  }
  
  public Boolean isActive() {
  	return enabled; // && isBoundary;
  }
  
  public DistributionElement(Integer freq, Boolean ena) {
    frequency = freq;
    enabled = ena;
  }
  
  public  DistributionElement(Integer freq) {
    frequency = freq;
  }

  @Override
  public int compareTo(DistributionElement o2) {
    return frequency.compareTo(o2.frequency);
  }
}