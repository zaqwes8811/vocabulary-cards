package dal.gae_kinds;

import business.mapreduce.ContentPageBuilder;
import business.nlp.ContentItemsTokenizer;
import business.text_extractors.SpecialSymbols;
import business.text_extractors.SubtitlesContentHandler;
import business.text_extractors.SubtitlesParser;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import common.Util;
import org.apache.tika.parser.Parser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.ContentHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static dal.gae_kinds.OfyService.ofy;
import static org.junit.Assert.assertFalse;

public class ContentPageTest {
  private static final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  private String getTestText(String filename) throws IOException {
    String rawText = Joiner.on('\n').join(Util.fileToList(filename));
    //InputStream in = closer.register(new FileInputStream(new File(filename)));  // No in GAE

    // Пока файл строго юникод - UTF-8
    Closer closer = Closer.create();
    try {
      // http://stackoverflow.com/questions/247161/how-do-i-turn-a-string-into-a-stream-in-java
      InputStream in = closer.register(new ByteArrayInputStream(rawText.getBytes(Charsets.UTF_8)));
      Parser parser = new SubtitlesParser();
      List<String> sink = new ArrayList<String>();
      ContentHandler handler = new SubtitlesContentHandler(sink);
      parser.parse(in, handler, null, null);

      // Получили список строк.
      SpecialSymbols symbols = new SpecialSymbols();
      return Joiner.on(symbols.WHITESPACE_STRING).join(sink);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  private ArrayList<ContentItem> getItems(String text) {
    ImmutableList<String> sentences = new ContentItemsTokenizer().getSentences(text);
    assertFalse(sentences.isEmpty());

    // Пакуем
    ArrayList<ContentItem> contentItems = new ArrayList<ContentItem>();
    Long idx = new Long(1);
    for (String sentence: sentences) {
      ContentItem item = new ContentItem(sentence);
      item.setIdx(idx);
      contentItems.add(item);
      idx++;
    }

    return contentItems;
  }

  private String getTestFileName() {
    return "/home/zaqwes/work/statistic/the.legend.of.korra.a.new.spiritual.age.(2013).eng.1cd.(5474296)/" +
      "The Legend of Korra - 02x10 - A New Spiritual Age.WEB-DL.BS.English.HI.C.orig.Addic7ed.com.srt";
  }

  @Test
  public void testCreateAndPersis() throws Exception {
    String filename = getTestFileName();

    // Phase I
    String text = getTestText(filename);
    assertFalse(text.isEmpty());

    // Phase II не всегда они разделены, но с случае с субтитрами точно разделены.
    ArrayList<ContentItem> contentItems = getItems(text);

    // Last - Persist page
    ContentPage page = new ContentPageBuilder().build("Korra", contentItems);
    ofy().save().entity(page).now();

    /// Queries
    // Получаем все сразу, но это никчему. Можно передать подсписок, но это не то что хотелось бы.
    // Хотелось бы выбирать по некоторому критерию.
    // https://groups.google.com/forum/#!topic/objectify-appengine/scb3xNPFszE
    // http://stackoverflow.com/questions/9867401/objectify-query-filter-by-list-in-entity-contains-search-parameter
    // http://bighow.net/3869301-Objectify___how_to__Load_a_List_lt_Ref_lt___gt__gt__.html
    //
    // http://stackoverflow.com/questions/11924572/using-in-query-in-objectify
    //
    // https://www.mail-archive.com/google-appengine-java@googlegroups.com/msg09389.html
    //
    // Заряжаем генератор
    //GeneratorAnyDistributionImpl gen = GeneratorAnyDistributionImpl.create(distribution);
    Integer idxPosition = 4;//gen.getPosition();
    int countFirst = 4;
    Word elem = ofy().load().type(Word.class).filter("sortedIdx =", idxPosition).first().get();
    List<ContentItem> coupled = ofy().load().type(ContentItem.class)
      .filterKey("in", elem.getItems())
      //.filter("idx <=", 8)
      .limit(countFirst)
      .list();
  }

  @Test
  public void testGetDistribution() {

  }

  @Test
  public void testDeletePage() {
    // TODO: Delete full page
  }
}
