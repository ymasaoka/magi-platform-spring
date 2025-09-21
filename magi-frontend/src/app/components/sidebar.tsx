import { NavLink } from "react-router";

export default function Sidebar() {
  return (
    <aside
      className="bg-light p-3 vh-100 position-fixed"
      style={{ width: "240px" }}
      aria-label="Sidebar"
    >
      <NavLink to="/" className="d-flex align-items-center mb-3 text-decoration-none">
        <span className="fs-4">MAGI</span>
      </NavLink>
      <hr />
      <ul className="nav nav-pills flex-column">
        <li className="nav-item">
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }
          >
            ホーム
          </NavLink>
        </li>
        <li className="nav-item">
          <NavLink
            to="/vote"
            className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }
          >
            投票
          </NavLink>
        </li>
        {/* 必要に応じてリンクを追加 */}
      </ul>
    </aside>
  );
}
