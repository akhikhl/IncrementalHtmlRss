package org.akhikhl.incrementalhtml

import com.rometools.fetcher.impl.HttpURLFeedFetcher
import com.rometools.fetcher.impl.HashMapFeedInfoCache
import com.rometools.rome.feed.synd.SyndFeed
import groovyx.javafx.GroovyFX
import javafx.beans.value.ChangeListener
import javafx.scene.web.WebView
import org.jsoup.Jsoup

class Main {

  static void main(String[] args) {
    new Main().run(args)
  }
  
  protected webv
  
  protected void run(String[] args) {
    GroovyFX.start { app ->
      stage(title: 'Incremental HTML example', visible: true, width: 1000, height: 618, resizable: true) {
        scene(stylesheets: resource('/css/default.css')) {
          webv = webView(styleClass: 'webv')
        }
        onShown {
          populateWebViewer()
        }
      }
    }
  }
  
  protected void populateWebViewer() {
    String css
    getClass().getResourceAsStream('/css/webv.css').withStream {
      css = it.text
    }
    webv.engine.loadContent("<html><head><style>$css</style></head><body></body></html>")
    webv.engine.loadWorker.stateProperty().addListener({ value, oldState, newState ->
      if (newState == javafx.concurrent.Worker.State.SUCCEEDED)
        fetchAndRenderRss()
    } as ChangeListener)
  }
  
  protected void fetchAndRenderRss() {
    SyndFeed feed = new HttpURLFeedFetcher(HashMapFeedInfoCache.getInstance()).retrieveFeed(new URL('http://news.google.com/news?ned=us&topic=h&output=rss'))
    use(groovy.xml.dom.DOMCategory) {
      def docElem = webv.engine.document.documentElement
      feed.entries.each { e ->
        def div = docElem.BODY[0].appendNode 'div', ['class': 'rssEntry']
        div.appendNode('a', ['class': 'title', href: e.link], e.title)
        if(e.description.type == 'text/html') {
          def doc = Jsoup.parse(e.description.value)
          for(def elem in doc.body().children())
            appendJsoupElemToDomParent(elem, div)
        }
      }
      docElem.BODY[0].lastChild.scrollIntoView(false)
    }
  }
  
  protected void appendJsoupElemToDomParent(def jsoupElem, def domParent) {
    def attrs = jsoupElem.attributes().collectEntries { it }
    if(jsoupElem.tagName() == 'img' && attrs.src?.startsWith('//'))
      attrs.src = 'http:' + attrs.src
    def text = jsoupElem.textNodes().collect { it.text() }.join()
    def domElem = domParent.appendNode(jsoupElem.tagName(), attrs, text)
    for(def jsoupChild in jsoupElem.children())
      appendJsoupElemToDomParent(jsoupChild, domElem)
  }
}

