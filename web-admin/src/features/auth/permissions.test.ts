import { describe, expect, it } from "vitest"
import {
  ADMIN_ONLY,
  STAFF,
  ROUTE_ROLES,
  ROLE_CAPABILITIES,
  canAccess,
  homeFor,
} from "./permissions"
import type { Role } from "@/features/admin/api"

describe("permissions", () => {
  describe("canAccess", () => {
    it("returns false when role is undefined", () => {
      expect(canAccess(undefined, "/")).toBe(false)
      expect(canAccess(undefined, "/customers")).toBe(false)
      expect(canAccess(undefined, "/kyc")).toBe(false)
      expect(canAccess(undefined, "/settings")).toBe(false)
    })

    it("allows ADMIN to access all defined routes", () => {
      const routes = [
        "/",
        "/customers",
        "/kyc",
        "/transfers",
        "/bill-providers",
        "/roles",
        "/audit-logs",
        "/settings",
      ]
      for (const route of routes) {
        expect(canAccess("ADMIN", route)).toBe(true)
      }
    })

    it("allows TELLER to access only customer and kyc routes", () => {
      expect(canAccess("TELLER", "/customers")).toBe(true)
      expect(canAccess("TELLER", "/kyc")).toBe(true)

      expect(canAccess("TELLER", "/")).toBe(false)
      expect(canAccess("TELLER", "/transfers")).toBe(false)
      expect(canAccess("TELLER", "/bill-providers")).toBe(false)
      expect(canAccess("TELLER", "/roles")).toBe(false)
      expect(canAccess("TELLER", "/audit-logs")).toBe(false)
      expect(canAccess("TELLER", "/settings")).toBe(false)
    })

    it("denies CUSTOMER role access to all routes", () => {
      const routes = Object.keys(ROUTE_ROLES)
      for (const route of routes) {
        expect(canAccess("CUSTOMER", route)).toBe(false)
      }
    })

    it("defaults unknown routes to ADMIN_ONLY", () => {
      expect(canAccess("ADMIN", "/unknown-route")).toBe(true)
      expect(canAccess("TELLER", "/unknown-route")).toBe(false)
      expect(canAccess("CUSTOMER", "/unknown-route")).toBe(false)
      expect(canAccess(undefined, "/unknown-route")).toBe(false)
    })
  })

  describe("homeFor", () => {
    it("returns /customers for TELLER", () => {
      expect(homeFor("TELLER")).toBe("/customers")
    })

    it("returns / for ADMIN", () => {
      expect(homeFor("ADMIN")).toBe("/")
    })

    it("returns / for CUSTOMER", () => {
      expect(homeFor("CUSTOMER")).toBe("/")
    })

    it("returns / for undefined role", () => {
      expect(homeFor(undefined)).toBe("/")
    })
  })

  describe("role mappings and constants", () => {
    it("defines ADMIN_ONLY and STAFF constants correctly", () => {
      expect(ADMIN_ONLY).toEqual(["ADMIN"])
      expect(STAFF).toEqual(["ADMIN", "TELLER"])
    })

    it("defines ROUTE_ROLES with appropriate role restrictions", () => {
      expect(ROUTE_ROLES["/"]).toBe(ADMIN_ONLY)
      expect(ROUTE_ROLES["/customers"]).toBe(STAFF)
      expect(ROUTE_ROLES["/kyc"]).toBe(STAFF)
      expect(ROUTE_ROLES["/transfers"]).toBe(ADMIN_ONLY)
      expect(ROUTE_ROLES["/bill-providers"]).toBe(ADMIN_ONLY)
      expect(ROUTE_ROLES["/roles"]).toBe(ADMIN_ONLY)
      expect(ROUTE_ROLES["/audit-logs"]).toBe(ADMIN_ONLY)
      expect(ROUTE_ROLES["/settings"]).toBe(ADMIN_ONLY)
    })

    it("provides capability descriptions for all known roles", () => {
      const roles: Role[] = ["ADMIN", "TELLER", "CUSTOMER"]
      expect(ROLE_CAPABILITIES).toHaveLength(3)

      for (const role of roles) {
        const capability = ROLE_CAPABILITIES.find((c) => c.role === role)
        expect(capability).toBeDefined()
        expect(capability?.summary).toBeTruthy()
        expect(capability?.can.length).toBeGreaterThan(0)
      }
    })
  })
})
