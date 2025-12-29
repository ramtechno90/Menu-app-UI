from playwright.sync_api import sync_playwright

def verify_portals():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # 1. Verify Admin Portal Load
        print("Verifying Admin Portal...")
        page.goto("http://localhost:8000/web_portal/admin/index.html")

        # Check for Landing Container (default visible)
        try:
            page.wait_for_selector("#landing-container", state="visible", timeout=5000)
            print("Admin Portal loaded: Landing container visible.")
        except Exception as e:
            print(f"Admin Portal load failed or timed out: {e}")

        # Check that we can navigate to Login
        # Assuming there is a button to go to login
        try:
             # Look for "Login as Admin" button logic.
             # Based on main.js: showAdminLogin() is called.
             # We can manually execute the JS function to test the transition logic
             page.evaluate("App.showAdminLogin()")
             page.wait_for_selector("#admin-login-container", state="visible", timeout=2000)
             print("Admin Portal: Transitioned to Login screen.")
        except Exception as e:
             print(f"Admin Portal transition failed: {e}")

        page.screenshot(path="verification/admin_portal_load.png")

        # 2. Verify Delivery Portal Load
        print("Verifying Delivery Portal...")
        page.goto("http://localhost:8000/web_portal/delivery/index.html")

        try:
            page.wait_for_selector("#landing-container", state="visible", timeout=5000)
            print("Delivery Portal loaded: Landing container visible.")
        except Exception as e:
            print(f"Delivery Portal load failed or timed out: {e}")

        try:
             page.evaluate("window.App.showDeliveryLogin()")
             page.wait_for_selector("#delivery-login-container", state="visible", timeout=2000)
             print("Delivery Portal: Transitioned to Login screen.")
        except Exception as e:
             print(f"Delivery Portal transition failed: {e}")

        page.screenshot(path="verification/delivery_portal_load.png")

        browser.close()

if __name__ == "__main__":
    verify_portals()
