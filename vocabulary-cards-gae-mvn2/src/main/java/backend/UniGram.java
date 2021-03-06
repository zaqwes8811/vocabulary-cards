package backend;

import com.google.common.collect.ImmutableList;
import backend.estimators.AdvImportanceProcessor;
import backend.estimators.ImportanceProcessor;

import java.util.*;

// TODO: Переименовать. Вообще хранятся не слова, а, например, стемы.
// Хранить их точно не буду - съест лимиты
public class UniGram {
	public static final Integer MAX_CONTENT_ITEMS_IN_PACK = 3;
	private final ImportanceProcessor estimator = new AdvImportanceProcessor();

  // TODO: может хранится стем или пара-тройка слов.
  private String value;

  // Сколько раз встретилось слово
  private Integer rawFrequency = 0;
  private Integer importance = 0;
  
  // May be make final
  private Set<ContentItem> smallInvertedIndex = new HashSet<>();

  public String getValue() {
  	return value;
  }

  public Integer getImportance() {
    return importance;
  }
  
  public void calcImportance() {
  	importance = estimator.process(rawFrequency, smallInvertedIndex);
  }

  // wrong but need
  public void setImportance(Integer value) {
    importance = value;
  }

  public static
  UniGram create(String value, Collection<ContentItem> sentences, int rawFrequency) {
    return new UniGram(value, sentences, rawFrequency);
  }

  public ImmutableList<ContentItem> getContendKinds() {
  	// FIXME: делать выборки с перемешиванием
  	ArrayList<ContentItem> tmp = new ArrayList<>(smallInvertedIndex);
  	
  	long seed = System.nanoTime();
  	Collections.shuffle(tmp, new Random(seed));
  	
  	if (tmp.isEmpty())
  		throw new IllegalStateException();
  	
  	Integer toIndex = MAX_CONTENT_ITEMS_IN_PACK;
  	if (tmp.size() < MAX_CONTENT_ITEMS_IN_PACK)
  		toIndex = tmp.size();
  	
  	if (tmp.isEmpty())
  		toIndex = 0;
  	
  	return ImmutableList.copyOf(tmp.subList(0, toIndex));
  }

  public UniGram(String value, Collection<ContentItem> smallInvertedIndex, int rawFrequency) {
    this.value = value;
    this.rawFrequency = rawFrequency;

    // FIXME: Ссылки должны быть уникальными. Но уникальны ли они тут?
    this.smallInvertedIndex.addAll(smallInvertedIndex);
  }

  private static class ImportanceComparator implements Comparator<UniGram> {
    // http://stackoverflow.com/questions/10017381/compareto-method-java
    //
    // In "Effective Java"
    @Override
    public int compare(UniGram o1, UniGram o2) {
      return o1.getImportance().compareTo(o2.getImportance());
    }
  }

  public static Comparator<UniGram> createImportanceComparator() {
    return new ImportanceComparator();
  }
}
