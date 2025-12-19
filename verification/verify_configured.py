
import os
from playwright.sync_api import sync_playwright, expect

def verify_configured_app():
    cwd = os.getcwd()
    app_path = f"file://{cwd}/web_portal/index.html"

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # 1. Load Page - Should go straight to landing (no setup)
        print(f"Navigating to {app_path}")
        page.goto(app_path)

        expect(page.get_by_text("Pizza Paradize Portal")).to_be_visible()
        # Verify setup is gone
        expect(page.get_by_text("Firebase Configuration Required")).not_to_be_visible()

        page.screenshot(path="/home/jules/verification/1_ready_landing.png")
        print("Landing verified (No Setup).")

        browser.close()

if __name__ == "__main__":
    verify_configured_app()
