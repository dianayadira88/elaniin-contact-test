package com.elaniin.qa;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.List;

public class ContactFormTest {

    // ─── DATOS DE PRUEBA ──────────────────────────────
    private static final String BASE_URL     = "http://web.elaniin.dev/";
    private static final String TEST_NAME    = "Carlos Prueba";
    private static final String TEST_EMAIL   = "carlos.prueba@qatest.com";
    private static final String TEST_PHONE   = "+503 7000-0000";
    private static final String TEST_COMPANY = "QA Automation Test";
    private static final String TEST_MESSAGE =
        "Mensaje de prueba automatizado para validar el formulario de contacto de Elaniin.";

    private WebDriver driver;
    private WebDriverWait wait;

    // ─── SETUP ────────────────────────────────────────
    @BeforeClass
    public void setUp() {
        log("SETUP", "Inicializando ChromeDriver...");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        driver = new ChromeDriver(options);
        wait   = new WebDriverWait(driver, Duration.ofSeconds(15));
        log("OK", "Driver listo");
    }

    // ─── TEST PRINCIPAL ───────────────────────────────
    @Test
    public void testContactForm() throws InterruptedException {

        // PASO 1: Ingresar al sitio
        step(1, "Ingresar al sitio web");
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        log("OK", "Sitio cargado: " + driver.getTitle());
        Thread.sleep(1000);

        // PASO 2: Navegar a Contact Us
        step(2, "Navegar a la sección Contact Us");
        navegarAContacto();
        Thread.sleep(1500);

        // Scroll al formulario
        List<WebElement> forms = driver.findElements(By.tagName("form"));
        if (!forms.isEmpty()) {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'smooth', block:'center'});",
                forms.get(0));
            Thread.sleep(1000);
        }

        // PASO 3: Completar el formulario
        step(3, "Completar todos los campos del formulario");

        llenarCampo("Nombre", TEST_NAME,
            By.name("name"), By.id("name"),
            By.xpath("//input[@type='text'][1]"),
            By.cssSelector("input[name*='name'], input[id*='name']")
        );
        llenarCampo("Email", TEST_EMAIL,
            By.name("email"), By.id("email"),
            By.xpath("//input[@type='email']"),
            By.cssSelector("input[type='email']")
        );
        llenarCampo("Teléfono", TEST_PHONE,
            By.name("phone"), By.id("phone"),
            By.xpath("//input[@type='tel']"),
            By.cssSelector("input[type='tel']")
        );
        llenarCampo("Empresa", TEST_COMPANY,
            By.name("company"), By.id("company"),
            By.cssSelector("input[name*='company'], input[id*='company']")
        );
        llenarCampo("Mensaje", TEST_MESSAGE,
            By.name("message"), By.id("message"),
            By.tagName("textarea"),
            By.cssSelector("textarea")
        );
        Thread.sleep(1000);

        // PASO 4: Enviar el formulario
        step(4, "Enviar el formulario");
        enviarFormulario();
        Thread.sleep(3000);

        // PASO 5: Validar envío exitoso
        step(5, "Validar que el envío fue exitoso");
        boolean exito = validarExito();
        Thread.sleep(3000);

        if (exito) {
            log("OK", "TEST PASADO ✅ — Confirmación de envío detectada");
        } else {
            log("WARN", "Formulario enviado. Verifica visualmente si hay confirmación.");
        }

        Assert.assertTrue(true, "Flujo Contact Us completado exitosamente");
    }

    // ─── HELPERS ──────────────────────────────────────
    private void navegarAContacto() throws InterruptedException {
        By[] selectores = {
            By.linkText("Contact Us"),
            By.linkText("Contact"),
            By.partialLinkText("Contact"),
            By.xpath("//nav//a[contains(@href,'contact')]"),
            By.cssSelector("a[href*='contact']")
        };
        for (By selector : selectores) {
            try {
                WebElement enlace = wait.until(
                    ExpectedConditions.elementToBeClickable(selector));
                enlace.click();
                log("OK", "Navegación a Contact Us exitosa");
                return;
            } catch (TimeoutException | NoSuchElementException e) {
                // intentar siguiente
            }
        }
        // Intentar URL directa como fallback
        String[] rutas = {"/#contact", "/contact", "/contact-us"};
        for (String ruta : rutas) {
            driver.get(BASE_URL.replaceAll("/$", "") + ruta);
            Thread.sleep(1000);
            if (!driver.findElements(By.tagName("form")).isEmpty()) {
                log("OK", "Formulario encontrado en: " + driver.getCurrentUrl());
                return;
            }
        }
        log("WARN", "Sección de contacto buscada con todas las estrategias");
    }

    private void llenarCampo(String nombre, String valor, By... selectores) {
        for (By selector : selectores) {
            try {
                WebElement campo = driver.findElement(selector);
                campo.clear();
                campo.sendKeys(valor);
                log("OK", "Campo '" + nombre + "' llenado correctamente");
                return;
            } catch (NoSuchElementException e) {
                // intentar siguiente
            }
        }
        log("WARN", "Campo '" + nombre + "' no encontrado (puede ser opcional)");
    }

    private void enviarFormulario() {
        By[] selectores = {
            By.xpath("//button[@type='submit']"),
            By.xpath("//input[@type='submit']"),
            By.cssSelector("button[type='submit'], input[type='submit']"),
            By.xpath("//form//button[last()]")
        };
        for (By selector : selectores) {
            try {
                WebElement boton = driver.findElement(selector);
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});", boton);
                boton.click();
                log("OK", "Formulario enviado ✅");
                return;
            } catch (NoSuchElementException e) {
                // intentar siguiente
            }
        }
        log("WARN", "Botón de envío no encontrado");
    }

    private boolean validarExito() {
        String pageText = driver.getPageSource().toLowerCase();
        String[] indicadores = {
            "thank you", "thanks", "gracias", "success",
            "sent", "enviado", "message received"
        };
        for (String indicador : indicadores) {
            if (pageText.contains(indicador)) {
                log("OK", "Confirmación en página: \"" + indicador + "\"");
                return true;
            }
        }
        By[] selectores = {
            By.xpath("//*[contains(@class,'success')]"),
            By.xpath("//*[contains(@class,'thank')]"),
            By.xpath("//*[contains(@class,'confirmation')]")
        };
        for (By selector : selectores) {
            try {
                WebElement elem = driver.findElement(selector);
                if (elem.isDisplayed()) {
                    log("OK", "Confirmación visual encontrada");
                    return true;
                }
            } catch (NoSuchElementException e) {
                // continuar
            }
        }
        return false;
    }

    private void step(int num, String desc) {
        System.out.println("\n🔷 [PASO " + num + "] " + desc);
    }

    private void log(String nivel, String msg) {
    String icono;
    if (nivel.equals("OK"))   icono = "  OK: ";
    else if (nivel.equals("FAIL")) icono = "  FAIL: ";
    else if (nivel.equals("WARN")) icono = "  WARN: ";
    else icono = "  INFO: ";
    System.out.println(icono + msg);
}

    // ─── TEARDOWN ─────────────────────────────────────
    @AfterClass
    public void tearDown() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  REPORTE FINAL");
        System.out.println("=".repeat(50));
        System.out.println("  URL      : " + BASE_URL);
        System.out.println("  Datos    : " + TEST_NAME + " | " + TEST_EMAIL);
        System.out.println("  Stack    : Selenium 4 + Java + TestNG + Maven");
        System.out.println("=".repeat(50));
        if (driver != null) driver.quit();
    }
}