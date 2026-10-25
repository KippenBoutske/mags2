package nl.cita.magister.auth

import java.awt.GraphicsEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import nl.cita.magister.ui.login.MagsJsBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

class M6loappFormHookTest {
    @Test
    fun injectJsPatchesPrototypeSubmitAndSubmitEvent() {
        assertTrue(M6loappFormHook.INJECT_JS.contains("HTMLFormElement.prototype"))
        assertTrue(M6loappFormHook.INJECT_JS.contains("proto.submit"))
        assertTrue(M6loappFormHook.INJECT_JS.contains("addEventListener('submit'"))
        assertTrue(M6loappFormHook.INJECT_JS.contains("m6loapp:"))
    }

    @Test
    fun parseCaptureResultReadsActionAndBody() {
        val parsed = M6loappFormHook.parseCaptureResult(
            "m6loapp://oauth2redirect/\ncode=abc&state=s",
        )
        assertNotNull(parsed)
        assertEquals("m6loapp://oauth2redirect/", parsed!!.first)
        assertEquals("code=abc&state=s", parsed.second)
        assertEquals(null, M6loappFormHook.parseCaptureResult(null))
        assertEquals(null, M6loappFormHook.parseCaptureResult("undefined"))
    }

    @Test
    fun webViewPrototypeSubmitCapturesCode() {
        Assume.assumeFalse("JavaFX WebView needs a display", GraphicsEnvironment.isHeadless())
        M6loappProtocol.install()
        val latch = CountDownLatch(1)
        var seen: String? = null
        M6loappProtocol.listener = { next ->
            seen = next
            latch.countDown()
        }
        startFx()
        val html = """
            <!DOCTYPE html><html><body>
            <form id="f" method="POST" action="m6loapp://oauth2redirect/">
              <input type="hidden" name="code" value="fromProtoSubmit"/>
              <input type="hidden" name="state" value="st"/>
            </form>
            <script>
              window.setTimeout(function() { document.forms[0].submit(); }, 200);
            </script>
            </body></html>
        """.trimIndent()
        Platform.runLater {
            val engine = WebView().engine
            engine.documentProperty().addListener { _, _, doc -> if (doc != null) inject(engine) }
            engine.loadWorker.stateProperty().addListener { _, _, state ->
                if (state == Worker.State.RUNNING || state == Worker.State.SUCCEEDED) inject(engine)
            }
            engine.loadContent(html)
        }
        assertTrue("WebView did not capture prototype.submit form_post", latch.await(20, TimeUnit.SECONDS))
        assertEquals("fromProtoSubmit", AuthRequestFactory.extractCode(seen!!))
    }

    private fun startFx() {
        val started = CountDownLatch(1)
        runCatching {
            Platform.startup { started.countDown() }
        }.onFailure {
            started.countDown()
        }
        started.await(10, TimeUnit.SECONDS)
        Platform.setImplicitExit(false)
    }

    private fun inject(engine: WebEngine) {
        runCatching {
            val window = engine.executeScript("window") as? JSObject
            window?.setMember("magsBridge", MagsJsBridge())
            engine.executeScript(M6loappFormHook.INJECT_JS)
        }
    }
}
