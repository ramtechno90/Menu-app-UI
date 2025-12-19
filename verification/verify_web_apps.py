
import os
from playwright.sync_api import sync_playwright, expect

def verify_web_apps():
    cwd = os.getcwd()
    admin_path = f"file://{cwd}/admin_web/index.html"
    delivery_path = f"file://{cwd}/delivery_staff_web/index.html"

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # Verify Admin App
        page = browser.new_page()
        print(f"Navigating to Admin App: {admin_path}")
        page.goto(admin_path)

        # Check title
        expect(page).to_have_title("Pizza Paradize Admin")

        # Check Login UI
        expect(page.get_by_text("Admin Login")).to_be_visible()
        expect(page.get_by_placeholder("Email")).to_be_visible()

        # Screenshot
        page.screenshot(path="/home/jules/verification/admin_login.png")
        print("Admin App verified.")

        # Verify Delivery App
        page = browser.new_page()
        print(f"Navigating to Delivery App: {delivery_path}")
        page.goto(delivery_path)

        # Check title
        expect(page).to_have_title("Delivery Staff Portal")

        # Check Login UI
        expect(page.get_by_text("Delivery Login")).to_be_visible()

        # Screenshot
        page.screenshot(path="/home/jules/verification/delivery_login.png")
        print("Delivery App verified.")

        browser.close()

if __name__ == "__main__":
    verify_web_apps()
