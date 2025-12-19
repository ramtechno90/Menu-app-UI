
import os
from playwright.sync_api import sync_playwright, expect

def verify_setup_flow():
    cwd = os.getcwd()
    app_path = f"file://{cwd}/web_portal/index.html"

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        # Need context with local storage access logic, but file:// protocol shares local storage in some browsers or isolates it.
        # Playwright isolates context.

        context = browser.new_context()
        page = context.new_page()

        # 1. First Load (No Config) -> Should see Setup
        print(f"Navigating to {app_path}")
        page.goto(app_path)

        # Check if setup container is visible
        expect(page.get_by_text("Firebase Configuration Required")).to_be_visible()
        page.screenshot(path="/home/jules/verification/1_setup_required.png")
        print("Setup Required screen verified.")

        # 2. Enter Config
        config_json = '{"apiKey": "TEST_KEY", "authDomain": "test", "projectId": "test"}'
        page.locator("#config-input").fill(config_json)
        page.get_by_text("Save Configuration").click()

        # 3. Reloads and should show Landing (though init will fail with fake key, but logic should proceed past setup check)
        # Note: In file:// protocol, reload might not persist localstorage in some headless envs or handle it differently.
        # But let's see. logic is location.reload().

        # Wait for reload? or check visibility
        # If init fails (TEST_KEY), firebase-config.js sets isConfigMissing = true in catch block?
        # My code:
        # try { initializeApp... } catch(e) { console.error... window.isConfigMissing = true; }

        # So if I provide a fake key that causes initializeApp to throw (it validates format?), it might go back to setup.
        # But 'TEST_KEY' might satisfy format check (string).
        # However, if it works, we see landing.

        # Actually, initializeApp doesn't check validity against server immediately. It just checks structure.
        # So it should pass and show Landing.

        expect(page.get_by_text("Pizza Paradize Portal")).to_be_visible()
        page.screenshot(path="/home/jules/verification/2_setup_complete.png")
        print("Setup completion verified.")

        browser.close()

if __name__ == "__main__":
    verify_setup_flow()
