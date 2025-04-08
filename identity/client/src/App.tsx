import { FC, StrictMode } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { getBaseUrl } from "./configuration";
import AppRoot from "./components/global/AppRoot";
import GlobalRoutes from "src/components/global/GlobalRoutes";
import { LoginPage } from "src/pages/login/LoginPage.tsx";
import Forbidden from "src/pages/forbidden/index.tsx";
import { NotificationProvider } from "src/components/notifications";

const App: FC = () => (
  <BrowserRouter basename={getBaseUrl()}>
    <StrictMode>
      <NotificationProvider>
        <Routes>
          <Route key="login" path="/login" Component={LoginPage} />
          <Route path="/forbidden" element={<Forbidden />} />
          <Route
            key="identity-ui"
            path="*"
            element={
              <AppRoot>
                <GlobalRoutes />
              </AppRoot>
            }
          />
        </Routes>
      </NotificationProvider>
    </StrictMode>
  </BrowserRouter>
);

export default App;
