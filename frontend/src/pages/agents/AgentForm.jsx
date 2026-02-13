import React, { useEffect, useMemo, useRef, useState } from "react";
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Button,
    Stack,
    Typography,
    CircularProgress,
    Tabs,
    Tab,
    Box,
    Autocomplete,
    Chip,
    Divider,
    MenuItem,
    Badge,
    Alert,
    Collapse,
    Checkbox,
    FormControlLabel,
    Paper,
    Tooltip,
    IconButton
} from "@mui/material";

import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";

import { safe, api } from "../../api/client";
import { listAIModels } from "../../api/AIModels";
import { listServers } from "../../api/mcpServers";

function TabPanel({ value, index, children }) {
    return (
        <div role="tabpanel" hidden={value !== index}>
            {value === index && <Box sx={{ pt: 2 }}>{children}</Box>}
        </div>
    );
}

// Debounce search (ms)
const SEARCH_DELAY = 350;
const PAGE_SIZE = 25;

// Version options: 1.0..1.9 ... 10.0
const VERSION_OPTIONS = (() => {
    const out = [];
    for (let major = 1; major <= 10; major++) {
        for (let minor = 0; minor <= 9; minor++) {
            if (major === 10 && minor > 0) break;
            out.push(`${major}.${minor}`);
        }
    }
    return out;
})();

const DISCOVERY_METHODS = ["GET", "POST", "PUT"];
const DISCOVERY_PROTOCOLS = ["HTTP", "HTTPS"];

const TEST_ENDPOINT_URL = "/v1/api/registry/agents/endpoint/test";

// GitHub repo validation (optional)
function isValidGithubRepoUrl(value) {
    const v = (value || "").trim();
    if (!v) return true;

    const withScheme = v.includes("://") ? v : `https://${v}`;

    try {
        const u = new URL(withScheme);
        if (u.protocol !== "http:" && u.protocol !== "https:") return false;
        if (!/^(www\.)?github\.com$/i.test(u.hostname)) return false;

        const parts = u.pathname.split("/").filter(Boolean);
        if (parts.length < 2) return false;

        const owner = parts[0];
        const repo = parts[1];

        const ownerOk =
            /^[A-Za-z0-9-]{1,39}$/.test(owner) && !owner.startsWith("-") && !owner.endsWith("-");
        const repoOk = /^[A-Za-z0-9._-]{1,100}$/.test(repo);

        return ownerOk && repoOk;
    } catch {
        return false;
    }
}

/**
 * Endpoint validator: host[:port][/path...]
 */
function isValidEndpointWithPath(input) {
    const raw = (input || "").trim();
    if (!raw) return false;

    if (raw.includes("://") || raw.includes("?") || raw.includes("#") || /\s/.test(raw)) return false;

    let hostPort = raw;
    let path = "";
    const slashIdx = raw.indexOf("/");
    if (slashIdx >= 0) {
        hostPort = raw.slice(0, slashIdx);
        path = raw.slice(slashIdx);
    }

    if (!hostPort) return false;

    const colonCount = (hostPort.match(/:/g) || []).length;
    if (colonCount > 1) return false;

    let host = hostPort;
    let port = null;

    if (colonCount === 1) {
        const parts = hostPort.split(":");
        host = parts[0];
        port = parts[1];

        if (!port || !/^\d{1,5}$/.test(port)) return false;
        const p = Number(port);
        if (p < 1 || p > 65535) return false;
    }

    if (!host) return false;
    if (host.length > 253) return false;

    const hostLower = host.toLowerCase();
    if (hostLower !== "localhost") {
        const ipv4Re =
            /^(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}$/;
        if (!ipv4Re.test(host)) {
            const labelRe = /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$/;
            const labels = host.split(".");
            for (const label of labels) {
                if (!labelRe.test(label)) return false;
            }
        }
    }

    if (path) {
        if (!path.startsWith("/")) return false;
        const pathRe = /^\/[A-Za-z0-9\-._~!$&'()*+,;=:@%\/]*$/;
        if (!pathRe.test(path)) return false;
    }

    return true;
}

// Accepts either raw JWT or "Bearer <jwt>"
function normalizeBearerToken(input) {
    const v = (input || "").trim();
    if (!v) return "";
    return v.toLowerCase().startsWith("bearer ") ? v.slice(7).trim() : v;
}

function isValidJwt(token) {
    const t = normalizeBearerToken(token);
    if (!t) return true; // optional
    const parts = t.split(".");
    if (parts.length !== 3) return false;

    const b64url = /^[A-Za-z0-9_-]+$/;
    return parts.every((p) => p.length > 0 && b64url.test(p));
}

// fingerprint: if changes => must re-test
function buildEndpointFingerprint({ method, protocol, endpoint, queryParam, bearerToken }) {
    const m = (method || "").trim().toUpperCase();
    const p = (protocol || "").trim().toUpperCase();
    const e = (endpoint || "").trim();
    const qp = (queryParam || "").trim();
    const bt = normalizeBearerToken(bearerToken || "");
    return `${m}|${p}|${e}|${qp}|${bt}`;
}

function TabLabelWithError({ label, hasError }) {
    if (!hasError) return label;
    return (
        <Badge
            variant="dot"
            color="error"
            overlap="circular"
            sx={{ "& .MuiBadge-badge": { right: -6, top: 6 } }}
        >
            <span>{label}</span>
        </Badge>
    );
}

// Helpers for tool selection keys
const toolKey = (serverId, toolName) => `${serverId}::${toolName}`;

// ---- NEW: Dirty-check helpers ----
function normStr(v) {
    return (v ?? "").toString().trim();
}

function setEquals(a, b) {
    if (a === b) return true;
    if (!a || !b) return false;
    if (a.size !== b.size) return false;
    for (const x of a) if (!b.has(x)) return false;
    return true;
}

// Tooltip helpers
function asText(v) {
    if (v === null || v === undefined) return "";
    if (typeof v === "string") return v;
    try {
        return JSON.stringify(v);
    } catch {
        return String(v);
    }
}

function renderToolTooltip(tool) {
    const desc = (tool?.description || "").trim();
    const args = tool?.arguments && typeof tool.arguments === "object" ? tool.arguments : null;

    const required = Array.isArray(args?.required) ? args.required : [];
    const props = args?.properties && typeof args.properties === "object" ? args.properties : {};

    const propKeys = Object.keys(props || {});

    return (
        <Box sx={{ maxWidth: 520, p: 0.5 }}>
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                {tool?.name || "Tool"}
                {tool?.version ? (
                    <Typography component="span" variant="caption" sx={{ opacity: 0.75 }}>
                        {" "}
                        (v{tool.version})
                    </Typography>
                ) : null}
            </Typography>

            {desc ? (
                <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", opacity: 0.9 }}>
                    {desc}
                </Typography>
            ) : (
                <Typography variant="body2" sx={{ opacity: 0.7 }}>
                    No description.
                </Typography>
            )}

            <Divider sx={{ my: 1 }} />

            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                Parameters
            </Typography>

            {propKeys.length === 0 ? (
                <Typography variant="body2" sx={{ opacity: 0.7 }}>
                    No parameters found.
                </Typography>
            ) : (
                <Stack spacing={1}>
                    {propKeys.map((k) => {
                        const p = props[k] || {};
                        const isReq = required.includes(k);

                        const type = p.type ? asText(p.type) : "";
                        const en = Array.isArray(p.enum) ? p.enum.map(String).join(", ") : "";
                        const def = p.default !== undefined ? asText(p.default) : "";
                        const pdesc = (p.description || "").trim();

                        return (
                            <Box key={k} sx={{ borderLeft: "2px solid", borderColor: "divider", pl: 1 }}>
                                <Stack direction="row" spacing={1} alignItems="baseline" sx={{ flexWrap: "wrap" }}>
                                    <Typography variant="body2" sx={{ fontWeight: 700 }}>
                                        {k}
                                    </Typography>
                                    {isReq ? (
                                        <Chip size="small" label="required" />
                                    ) : (
                                        <Chip size="small" variant="outlined" label="optional" />
                                    )}
                                    {type ? (
                                        <Typography variant="caption" sx={{ opacity: 0.75 }}>
                                            type: {type}
                                        </Typography>
                                    ) : null}
                                </Stack>

                                {pdesc ? (
                                    <Typography variant="caption" sx={{ display: "block", opacity: 0.85, whiteSpace: "pre-wrap" }}>
                                        {pdesc}
                                    </Typography>
                                ) : null}

                                {en ? (
                                    <Typography variant="caption" sx={{ display: "block", opacity: 0.8 }}>
                                        enum: {en}
                                    </Typography>
                                ) : null}

                                {def ? (
                                    <Typography variant="caption" sx={{ display: "block", opacity: 0.8 }}>
                                        default: {def}
                                    </Typography>
                                ) : null}
                            </Box>
                        );
                    })}
                </Stack>
            )}
        </Box>
    );
}

export default function AgentForm({ open, onClose, onSave, initial }) {
    const [tab, setTab] = useState(0);

    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [version, setVersion] = useState("1.0");
    const [githubRepoUrl, setGithubRepoUrl] = useConfirmEmptyString(initial?.githubRepoUrl);

    // Endpoint / discovery
    const [discoveryMethod, setDiscoveryMethod] = useState("POST");
    const [discoveryProtocol, setDiscoveryProtocol] = useState("HTTPS");
    const [discoveryEndpoint, setDiscoveryEndpoint] = useState("");
    const [discoveryQueryParam, setDiscoveryQueryParam] = useState("");

    // Security
    const [bearerToken, setBearerToken] = useState("");
    const [bearerTouched, setBearerTouched] = useState(false);

    // Touch flags
    const [nameTouched, setNameTouched] = useState(false);
    const [descTouched, setDescTouched] = useState(false);
    const [modelsTouched, setModelsTouched] = useState(false);
    const [githubTouched, setGithubTouched] = useState(false);

    const [endpointTouched, setEndpointTouched] = useState(false);
    const [queryParamTouched, setQueryParamTouched] = useState(false);

    // Selected models
    const [selectedModels, setSelectedModels] = useState([]);

    // Models dropdown
    const [modelsOpen, setModelsOpen] = useState(false);
    const [modelsLoading, setModelsLoading] = useState(false);

    const [modelSearchInput, setModelSearchInput] = useState("");
    const [modelSearchQuery, setModelSearchQuery] = useState("");

    const [modelOptions, setModelOptions] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const loadingRef = useRef(false);

    const [saving, setSaving] = useState(false);

    // Endpoint test state
    const [testing, setTesting] = useState(false);
    const [endpointTest, setEndpointTest] = useState({
        status: "idle", // "idle" | "ok" | "failed"
        error: null,
        result: null
    });

    // “what endpoint config is currently verified”
    const [verifiedEndpointFp, setVerifiedEndpointFp] = useState(null);

    // ✅ NEW: snapshot inicial para dirty-check
    const initialSnapshotRef = useRef(null);

    // Tools tab state
    const [mcpLoading, setMcpLoading] = useState(false);
    const [mcpError, setMcpError] = useState(null);
    const [mcpServers, setMcpServers] = useState([]);
    const [selectedToolKeys, setSelectedToolKeys] = useState(() => new Set());

    const modelLabel = (m) => (m ? m.modelName || m.modelId || "" : "");

    // Derived validations
    const nameTrim = name.trim();
    const descTrim = description.trim();
    const githubTrim = (githubRepoUrl || "").trim();

    const hasModels = selectedModels.length > 0;

    const endpointTrim = discoveryEndpoint.trim();
    const queryParamTrim = discoveryQueryParam.trim();

    const endpointIsValid = isValidEndpointWithPath(endpointTrim);
    const queryParamIsValid = queryParamTrim.length > 0;

    const githubIsValid = isValidGithubRepoUrl(githubTrim);

    const bearerTrim = bearerToken.trim();
    const bearerIsValid = isValidJwt(bearerTrim);

    const generalValid =
        nameTrim.length > 0 && version.trim().length > 0 && descTrim.length > 0 && hasModels && githubIsValid;

    const endpointValid =
        discoveryMethod.trim().length > 0 &&
        discoveryProtocol.trim().length > 0 &&
        endpointTrim.length > 0 &&
        endpointIsValid &&
        queryParamIsValid &&
        bearerIsValid;

    // Inline errors
    const nameError = nameTouched && nameTrim.length === 0;
    const descError = descTouched && descTrim.length === 0;
    const modelsError = modelsTouched && !hasModels;
    const githubError = githubTouched && !githubIsValid;

    const endpointError = endpointTouched && !endpointIsValid;
    const queryParamError = queryParamTouched && !queryParamIsValid;
    const bearerError = bearerTouched && !bearerIsValid;

    // Tab error indicators
    const generalTabError = !generalValid && (nameTouched || descTouched || modelsTouched || githubTouched);
    const endpointTabError = !endpointValid && (endpointTouched || queryParamTouched || bearerTouched);

    const currentEndpointFp = useMemo(
        () =>
            buildEndpointFingerprint({
                method: discoveryMethod,
                protocol: discoveryProtocol,
                endpoint: discoveryEndpoint,
                queryParam: discoveryQueryParam,
                bearerToken
            }),
        [discoveryMethod, discoveryProtocol, discoveryEndpoint, discoveryQueryParam, bearerToken]
    );

    const endpointNeedsVerification = !verifiedEndpointFp || currentEndpointFp !== verifiedEndpointFp;

    // ✅ NEW: isDirty (solo habilitar Save si hubo algún cambio)
    const isDirty = useMemo(() => {
        const snap = initialSnapshotRef.current;
        if (!snap) return false;

        const nowModelsKey = new Set(
            (selectedModels || []).map(
                (m) => `${normStr(m?.modelId)}|${normStr(m?.provider)}|${normStr(m?.modelName)}`
            )
        );

        const generalChanged =
            normStr(name) !== snap.name ||
            normStr(description) !== snap.description ||
            normStr(version) !== snap.version ||
            normStr(githubRepoUrl) !== snap.githubRepoUrl ||
            !setEquals(nowModelsKey, snap.modelsKey);

        const toolsChanged = !setEquals(selectedToolKeys, snap.toolsKey);

        const endpointChanged = currentEndpointFp !== snap.endpointFp;

        return generalChanged || toolsChanged || endpointChanged;
    }, [name, description, version, githubRepoUrl, selectedModels, selectedToolKeys, currentEndpointFp]);

    useEffect(() => {
        if (!open) return;

        setTab(0);

        if (initial) {
            setName(initial.name || "");
            setDescription(initial.description || "");
            setVersion(initial.version || "1.0");
            setGithubRepoUrl(initial.githubRepoUrl || "");

            const llms = initial?.metadata?.llms || [];
            const resolved = (Array.isArray(llms) ? llms : []).map((m) => ({
                modelId: m.id,
                modelName: m.modelName,
                modelDescription: "",
                provider: m.modelFamily
            }));
            setSelectedModels(resolved);

            const d = initial?.metadata?.discovery || {};
            const method = (d.method || "POST").toUpperCase();
            const protocol = (d.protocol || "HTTPS").toUpperCase();
            setDiscoveryMethod(method);
            setDiscoveryProtocol(protocol);
            setDiscoveryEndpoint(d.endpoint || "");
            setDiscoveryQueryParam(d.queryParam || "");

            const sec = initial?.metadata?.security || {};
            setBearerToken(sec?.bearerToken || "");

            const tools = initial?.metadata?.tools || [];
            const keys = new Set();
            if (Array.isArray(tools)) {
                for (const t of tools) {
                    const sid = t?.mcpServerId || t?.mcp_server_id || t?.serverId || t?.server_id;
                    const tn = t?.toolName || t?.tool_name || t?.name;
                    if (sid && tn) keys.add(toolKey(String(sid), String(tn)));
                }
            }
            setSelectedToolKeys(keys);

            // In EDIT: mark endpoint as verified
            const initFp = buildEndpointFingerprint({
                method,
                protocol,
                endpoint: d.endpoint || "",
                queryParam: d.queryParam || "",
                bearerToken: sec?.bearerToken || ""
            });
            setVerifiedEndpointFp(initFp);

            // Optional: show "Verified" state
            setEndpointTest({ status: "ok", error: null, result: null });

            // ✅ Snapshot inicial (EDIT)
            initialSnapshotRef.current = {
                name: normStr(initial?.name || ""),
                description: normStr(initial?.description || ""),
                version: normStr(initial?.version || "1.0"),
                githubRepoUrl: normStr(initial?.githubRepoUrl || ""),
                modelsKey: new Set(
                    ((initial?.metadata?.llms || []) || []).map(
                        (m) => `${normStr(m?.id)}|${normStr(m?.modelFamily)}|${normStr(m?.modelName)}`
                    )
                ),
                toolsKey: new Set(keys),
                endpointFp: initFp
            };
        } else {
            // CREATE defaults
            setName("");
            setDescription("");
            setVersion("1.0");
            setGithubRepoUrl("");
            setSelectedModels([]);

            setDiscoveryMethod("POST");
            setDiscoveryProtocol("HTTPS");
            setDiscoveryEndpoint("");
            setDiscoveryQueryParam("");

            setBearerToken("");
            setSelectedToolKeys(new Set());

            setVerifiedEndpointFp(null);
            setEndpointTest({ status: "idle", error: null, result: null });

            // ✅ Snapshot inicial (CREATE)
            initialSnapshotRef.current = {
                name: "",
                description: "",
                version: "1.0",
                githubRepoUrl: "",
                modelsKey: new Set(),
                toolsKey: new Set(),
                endpointFp: buildEndpointFingerprint({
                    method: "POST",
                    protocol: "HTTPS",
                    endpoint: "",
                    queryParam: "",
                    bearerToken: ""
                })
            };
        }

        setNameTouched(false);
        setDescTouched(false);
        setModelsTouched(false);
        setGithubTouched(false);
        setEndpointTouched(false);
        setQueryParamTouched(false);
        setBearerTouched(false);

        setModelSearchInput("");
        setModelSearchQuery("");
        setModelOptions([]);
        setPage(0);
        setHasMore(true);
        loadingRef.current = false;

        setTesting(false);

        setMcpLoading(false);
        setMcpError(null);
        setMcpServers([]);
    }, [open, initial, setGithubRepoUrl]);

    useEffect(() => {
        const t = setTimeout(() => {
            setModelSearchQuery(modelSearchInput.trim());
            setPage(0);
            setHasMore(true);
            setModelOptions([]);
        }, SEARCH_DELAY);

        return () => clearTimeout(t);
    }, [modelSearchInput]);

    const fetchModelsPage = async ({ pageToLoad }) => {
        if (loadingRef.current) return;
        if (!hasMore && pageToLoad !== 0) return;

        loadingRef.current = true;
        setModelsLoading(true);

        const params = {
            search: modelSearchQuery || undefined,
            page: pageToLoad,
            size: PAGE_SIZE,
            sortBy: "provider",
            sortDir: "asc"
        };

        const r = await safe(() => listAIModels(params));

        if (r.ok) {
            const content = Array.isArray(r.data?.content) ? r.data.content : [];
            const totalPages = typeof r.data?.totalPages === "number" ? r.data.totalPages : 0;

            setModelOptions((prev) => {
                const map = new Map(prev.map((x) => [x.modelId, x]));
                for (const m of content) map.set(m.modelId, m);
                return Array.from(map.values());
            });

            setHasMore(pageToLoad + 1 < totalPages);
            setPage(pageToLoad);
        } else {
            console.error("Error loading models:", r.error);
            setHasMore(false);
        }

        setModelsLoading(false);
        loadingRef.current = false;
    };

    useEffect(() => {
        if (!modelsOpen) return;
        fetchModelsPage({ pageToLoad: 0 });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [modelsOpen, modelSearchQuery]);

    const handleListboxScroll = (event) => {
        const listboxNode = event.currentTarget;
        const { scrollTop, scrollHeight, clientHeight } = listboxNode;

        if (scrollHeight - scrollTop - clientHeight < 60) {
            if (!modelsLoading && hasMore) {
                fetchModelsPage({ pageToLoad: page + 1 });
            }
        }
    };

    const goToFirstInvalidTab = () => {
        if (!generalValid) return setTab(0);
        if (!endpointValid) return setTab(2);
    };

    // If endpoint differs from verified => clear test; if matches verified => keep ok
    useEffect(() => {
        if (endpointNeedsVerification) {
            setEndpointTest({ status: "idle", error: null, result: null });
        } else {
            setEndpointTest((prev) =>
                prev?.status === "ok" ? prev : { status: "ok", error: null, result: prev?.result || null }
            );
        }
    }, [endpointNeedsVerification]);

    const loadMcpServers = async () => {
        if (mcpLoading) return;
        setMcpLoading(true);
        setMcpError(null);

        const r = await safe(() => listServers({ page: 0, size: 500, sortBy: "name", sortDir: "asc" }));
        if (r.ok) {
            const data = Array.isArray(r.data) ? r.data : Array.isArray(r.data?.content) ? r.data.content : [];

            // Filtra los servidores para marcar solo aquellos que no son "ACTIVE"
            const updatedServers = data.map((srv) => ({
                ...srv,
                isActive: srv.status && srv.status.toUpperCase() === "ACTIVE"
            }));

            setMcpServers(updatedServers); // Actualiza el estado con los servidores modificados
        } else {
            const msg =
                r.error?.message ||
                r.error?.response?.data?.message ||
                r.error?.response?.data?.error ||
                "Failed to load MCP servers.";
            setMcpError(msg);
            setMcpServers([]);
        }

        setMcpLoading(false);
    };


    useEffect(() => {
        if (!open) return;
        if (tab !== 1) return;
        if (mcpServers.length > 0 || mcpLoading) return;
        loadMcpServers();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tab, open]);

    const getServerToolNames = (server) => {
        const tools = Array.isArray(server?.tools) ? server.tools : [];
        return tools.map((t) => String(t?.name || "")).filter(Boolean);
    };

    const getServerSelectionState = (server) => {
        const serverId = String(server?.serverId || server?.server_id || "");
        const toolNames = getServerToolNames(server);
        if (!serverId || toolNames.length === 0) return { checked: false, indeterminate: false };

        let selectedCount = 0;
        for (const tn of toolNames) {
            if (selectedToolKeys.has(toolKey(serverId, tn))) selectedCount++;
        }
        const checked = selectedCount === toolNames.length;
        const indeterminate = selectedCount > 0 && selectedCount < toolNames.length;
        return { checked, indeterminate };
    };

    const getSelectedCountForServer = (server) => {
  const serverId = String(server?.serverId || server?.server_id || "");
  const toolNames = getServerToolNames(server);
  if (!serverId || toolNames.length === 0) return 0;

  let selectedCount = 0;
  for (const tn of toolNames) {
    if (selectedToolKeys.has(toolKey(serverId, tn))) selectedCount++;
  }
  return selectedCount;
};

const clearServerSelections = (server) => {
  const serverId = String(server?.serverId || server?.server_id || "");
  const toolNames = getServerToolNames(server);
  if (!serverId || toolNames.length === 0) return;

  setSelectedToolKeys((prev) => {
    const next = new Set(prev);
    for (const tn of toolNames) next.delete(toolKey(serverId, tn));
    return next;
  });
};


    const toggleServer = (server, nextChecked) => {
        const serverId = String(server?.serverId || server?.server_id || "");
        const toolNames = getServerToolNames(server);

        setSelectedToolKeys((prev) => {
            const next = new Set(prev);
            for (const tn of toolNames) {
                const k = toolKey(serverId, tn);
                if (nextChecked) next.add(k);
                else next.delete(k);
            }
            return next;
        });
    };

    const toggleTool = (serverId, toolName, nextChecked) => {
        setSelectedToolKeys((prev) => {
            const next = new Set(prev);
            const k = toolKey(serverId, toolName);
            if (nextChecked) next.add(k);
            else next.delete(k);
            return next;
        });
    };

    const handleSave = async () => {
        setNameTouched(true);
        setDescTouched(true);
        setModelsTouched(true);
        setGithubTouched(true);
        setEndpointTouched(true);
        setQueryParamTouched(true);
        setBearerTouched(true);

        if (!generalValid || !endpointValid) {
            goToFirstInvalidTab();
            alert("Please complete all required fields in General and Endpoint tabs.");
            return;
        }

        if (!isDirty) {
            // defensivo: no debería ocurrir porque el botón está deshabilitado
            alert("No changes to save.");
            return;
        }

        // Only require test when endpoint changed (or never verified)
        if (endpointNeedsVerification && endpointTest.status !== "ok") {
            setTab(2);
            alert("Endpoint changed. Please run “Test Connection” successfully before saving.");
            return;
        }

        setSaving(true);

        const llmsPayload = (selectedModels || []).map((m) => ({
            id: m.modelId,
            modelFamily: m.provider ? String(m.provider) : "AIModel",
            modelName: m.modelName
        }));

        const discoveryPayload = {
            method: discoveryMethod,
            protocol: discoveryProtocol,
            endpoint: endpointTrim,
            queryParam: queryParamTrim
        };

        const normalizedBearer = normalizeBearerToken(bearerToken);

        const serverMap = new Map((mcpServers || []).map((s) => [String(s.serverId), s]));

        const toolsPayload = Array.from(selectedToolKeys).map((k) => {
            const [sid, tn] = k.split("::");
            const srv = serverMap.get(String(sid));
            return {
                mcpServerId: String(sid),
                mcpServerName: srv?.name || null,
                mcpServerVersion: srv?.version || null,
                toolName: String(tn)
            };
        });

        const agentData = {
            name: nameTrim,
            description: descTrim,
            version: version.trim(),
            githubRepoUrl: githubTrim || "",
            metadata: {
                ...(initial?.metadata || {}),
                llms: llmsPayload,
                discovery: discoveryPayload,
                security: normalizedBearer ? { bearerToken: normalizedBearer } : {},
                tools: toolsPayload
            }
        };

        try {
            await onSave(agentData);
            onClose();
        } catch (error) {
            console.error("Error al guardar el agente:", error);
            alert("Error al guardar el agente");
        } finally {
            setSaving(false);
        }
    };

    const handleTestConnection = async () => {
        setBearerTouched(true);
        setEndpointTouched(true);
        setQueryParamTouched(true);

        if (!endpointValid) return;

        setTesting(true);
        setEndpointTest({ status: "idle", error: null, result: null });

        const normalizedBearer = normalizeBearerToken(bearerToken);

        const payload = {
            method: discoveryMethod,
            protocol: discoveryProtocol,
            endpoint: endpointTrim,
            queryParam: queryParamTrim,
            query: "test",
            ...(normalizedBearer ? { bearerToken: normalizedBearer } : {})
        };

        const r = await safe(() => api.post(TEST_ENDPOINT_URL, payload).then((res) => res.data));

        if (r.ok) {
            const ok = !!r.data?.ok;

            setEndpointTest({
                status: ok ? "ok" : "failed",
                error: ok ? null : r.data?.message || "Endpoint test returned non-ok.",
                result: r.data
            });

            if (ok) {
                setVerifiedEndpointFp(
                    buildEndpointFingerprint({
                        method: discoveryMethod,
                        protocol: discoveryProtocol,
                        endpoint: endpointTrim,
                        queryParam: queryParamTrim,
                        bearerToken
                    })
                );
            }
        } else {
            const msg =
                r.error?.message ||
                r.error?.response?.data?.message ||
                r.error?.response?.data?.error ||
                "Endpoint test failed.";
            setEndpointTest({ status: "failed", error: msg, result: null });
        }

        setTesting(false);
    };

    const saveDisabled =
        saving ||
        !isDirty || // ✅ clave: no habilitar si no hubo cambios
        !generalValid ||
        !endpointValid ||
        (endpointNeedsVerification && endpointTest.status !== "ok");

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle sx={{ pb: 1 }}>
                <Typography variant="h6">{initial ? "Edit Agent" : "Create Agent"}</Typography>
            </DialogTitle>

            <DialogContent sx={{ pt: 1 }}>
                <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: "divider" }}>
                    <Tab label={<TabLabelWithError label="General" hasError={generalTabError} />} />
                    <Tab label="Tools" />
                    <Tab label={<TabLabelWithError label="Endpoint" hasError={endpointTabError} />} />
                </Tabs>

                {/* General Tab */}
                <TabPanel value={tab} index={0}>
                    <Stack spacing={2}>
                        <TextField
                            label="Name"
                            fullWidth
                            required
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            onBlur={() => setNameTouched(true)}
                            error={nameError}
                            helperText={nameError ? "Required." : ""}
                        />

                        <TextField
                            select
                            label="Version"
                            fullWidth
                            required
                            value={version}
                            onChange={(e) => setVersion(e.target.value)}
                            SelectProps={{ MenuProps: { PaperProps: { style: { maxHeight: 320 } } } }}
                        >
                            {VERSION_OPTIONS.map((v) => (
                                <MenuItem key={v} value={v}>
                                    {v}
                                </MenuItem>
                            ))}
                        </TextField>

                        <TextField
                            label="Description"
                            fullWidth
                            required
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            onBlur={() => setDescTouched(true)}
                            multiline
                            minRows={3}
                            error={descError}
                            helperText={descError ? "Required." : ""}
                        />

                        <Autocomplete
                            multiple
                            open={modelsOpen}
                            onOpen={() => setModelsOpen(true)}
                            onClose={() => setModelsOpen(false)}
                            options={modelOptions}
                            value={selectedModels}
                            onChange={(_, v) => {
                                setSelectedModels(v);
                                setModelsTouched(true);
                            }}
                            getOptionLabel={modelLabel}
                            isOptionEqualToValue={(a, b) => a?.modelId === b?.modelId}
                            filterOptions={(x) => x}
                            onInputChange={(_, value, reason) => {
                                if (reason === "input" || reason === "clear") setModelSearchInput(value);
                            }}
                            renderTags={(value, getTagProps) =>
                                value.map((option, index) => (
                                    <Chip
                                        label={option.provider ? `${option.provider} / ${option.modelName}` : option.modelName}
                                        {...getTagProps({ index })}
                                        key={option.modelId || index}
                                        size="small"
                                    />
                                ))
                            }
                            ListboxProps={{
                                onScroll: handleListboxScroll,
                                style: { maxHeight: 320, overflow: "auto" }
                            }}
                            loading={modelsLoading}
                            renderInput={(params) => (
                                <TextField
                                    {...params}
                                    label="Associated Models"
                                    required
                                    error={modelsError}
                                    helperText={modelsError ? "Select at least one model." : ""}
                                    placeholder="Search and select models..."
                                    onBlur={() => setModelsTouched(true)}
                                    InputProps={{
                                        ...params.InputProps,
                                        endAdornment: (
                                            <>
                                                {modelsLoading ? <CircularProgress color="inherit" size={18} /> : null}
                                                {params.InputProps.endAdornment}
                                            </>
                                        )
                                    }}
                                />
                            )}
                        />

                        <Divider sx={{ my: 1 }} />

                        <TextField
                            label="GitHub Repo URL"
                            fullWidth
                            value={githubRepoUrl}
                            onChange={(e) => setGithubRepoUrl(e.target.value)}
                            onBlur={() => setGithubTouched(true)}
                            error={githubError}
                            helperText={
                                githubError
                                    ? "Invalid GitHub repository URL. Example: https://github.com/org/repo"
                                    : "Optional. Example: https://github.com/org/repo"
                            }
                            placeholder="https://github.com/org/repo"
                        />
                    </Stack>
                </TabPanel>

                {/* Tools Tab */}
                <TabPanel value={tab} index={1}>
                    <Stack spacing={2}>
                        <Stack direction="row" justifyContent="space-between" alignItems="center">
                            <Typography variant="subtitle1">MCP Tools</Typography>
                            <Button variant="outlined" size="small" onClick={loadMcpServers} disabled={mcpLoading}>
                                {mcpLoading ? (
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <CircularProgress size={16} color="inherit" />
                                        <span>Refreshing...</span>
                                    </Stack>
                                ) : (
                                    "Refresh"
                                )}
                            </Button>
                        </Stack>

                        <Typography variant="body2" sx={{ opacity: 0.75 }}>
                            Select MCP Servers and/or individual tools. Selecting a server selects all its tools.
                        </Typography>

                        <Collapse in={!!mcpError}>
                            <Alert severity="error">{mcpError}</Alert>
                        </Collapse>

                        <Paper variant="outlined" sx={{ p: 1.25, maxHeight: 420, overflow: "auto" }}>
                            {mcpLoading ? (
                                <Stack alignItems="center" sx={{ py: 4 }}>
                                    <CircularProgress />
                                    <Typography variant="caption" sx={{ mt: 1, opacity: 0.75 }}>
                                        Loading MCP servers...
                                    </Typography>
                                </Stack>
                            ) : (mcpServers || []).length === 0 ? (
                                <Typography variant="body2" sx={{ opacity: 0.7, p: 1 }}>
                                    No MCP servers found.
                                </Typography>
                            ) : (
                                <Stack spacing={1}>
                                    {mcpServers.map((srv) => {
                                        const serverId = String(srv.serverId);
                                        const tools = Array.isArray(srv.tools) ? srv.tools : [];
                                        const sel = getServerSelectionState(srv);

                                        const isActive = srv.isActive; // Indica si el servidor está activo
                                        const statusLabel = isActive ? "ACTIVE" : "INACTIVE"; // Etiqueta de estado

                                        const selectedCount = getSelectedCountForServer(srv);
                                        const hasSelections = selectedCount > 0;

                                        return (
                                            <Box
                                                key={serverId}
                                                sx={{
                                                    pb: 1,
                                                    opacity: isActive ? 1 : 0.45, // Si el servidor no es activo, lo ponemos gris
                                                    filter: isActive ? "none" : "grayscale(1)", // Aseguramos que se vea en gris
                                                    transition: "opacity 120ms ease"
                                                }}
                                            >
                            <Tooltip title={`Server status: ${statusLabel}`} arrow disableHoverListener={isActive}>
  <Box>
    <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ gap: 1 }}>
      <FormControlLabel
        disabled={!isActive} // sigue sin permitir seleccionar cuando está inactivo
        control={
          <Checkbox
            disabled={!isActive} // idem
            checked={sel.checked}
            indeterminate={sel.indeterminate}
            onChange={(e) => toggleServer(srv, e.target.checked)}
          />
        }
        label={
          <Stack spacing={0.25}>
            <Typography variant="subtitle2" sx={{ lineHeight: 1.1 }}>
              {srv.name}{" "}
              <Typography component="span" variant="caption" sx={{ opacity: 0.65 }}>
                (v{srv.version})
              </Typography>
            </Typography>

            <Stack direction="row" spacing={1} alignItems="center">
              <Chip
                size="small"
                label={statusLabel}
                variant={isActive ? "filled" : "outlined"}
              />
              {hasSelections ? (
                <Typography variant="caption" sx={{ opacity: 0.75 }}>
                  {selectedCount} selected
                </Typography>
              ) : null}
            </Stack>
          </Stack>
        }
      />

      {/* ✅ NUEVO: permitir DESVINCULAR si está inactivo y había selecciones */}
      {!isActive && hasSelections ? (
        <Button
          size="small"
          variant="outlined"
          onClick={() => clearServerSelections(srv)}
          sx={{
            // para que sea claramente clickeable aunque el resto esté gris
            opacity: 1,
            filter: "none",
            whiteSpace: "nowrap"
          }}
        >
          Unlink server
        </Button>
      ) : null}
    </Stack>
  </Box>
</Tooltip>


                                                <Stack spacing={0.25} sx={{ pl: 4 }}>
                                                    {tools.length === 0 ? (
                                                        <Typography variant="caption" sx={{ opacity: 0.6 }}>
                                                            No tools in this server.
                                                        </Typography>
                                                    ) : (
                                                        tools.map((t) => {
                                                            const tn = String(t?.name || "");
                                                            const checked = selectedToolKeys.has(toolKey(serverId, tn));
                                                            const tooltip = renderToolTooltip(t);

                                                            return (
                                                                <FormControlLabel
                                                                    key={toolKey(serverId, tn)}
                                                                    disabled={!isActive} // Deshabilita la selección si el servidor no está activo
                                                                    control={
                                                                        <Checkbox
                                                                            disabled={!isActive} // Deshabilita la selección de la herramienta si el servidor no está activo
                                                                            checked={checked}
                                                                            onChange={(e) => toggleTool(serverId, tn, e.target.checked)}
                                                                        />
                                                                    }
                                                                    label={
                                                                        <Stack direction="row" spacing={1} alignItems="center" sx={{ minWidth: 0 }}>
                                                                            <Stack sx={{ minWidth: 0 }}>
                                                                                <Typography variant="body2" sx={{ lineHeight: 1.1 }}>
                                                                                    {tn}{" "}
                                                                                    <Typography component="span" variant="caption" sx={{ opacity: 0.65 }}>
                                                                                        (v{t?.version || "n/a"})
                                                                                    </Typography>
                                                                                </Typography>
                                                                                {t?.description ? (
                                                                                    <Typography variant="caption" sx={{ opacity: 0.7 }}>
                                                                                        {String(t.description).split("\n")[0]}
                                                                                    </Typography>
                                                                                ) : null}
                                                                            </Stack>

                                                                            <Tooltip arrow placement="right" enterDelay={250} leaveDelay={100} title={tooltip}>
                                                                                <IconButton size="small" sx={{ opacity: 0.75 }}>
                                                                                    <InfoOutlinedIcon fontSize="inherit" />
                                                                                </IconButton>
                                                                            </Tooltip>
                                                                        </Stack>
                                                                    }
                                                                />
                                                            );
                                                        })
                                                    )}
                                                </Stack>

                                                <Divider sx={{ mt: 1 }} />
                                            </Box>
                                        );
                                    })}
                                </Stack>
                            )}
                        </Paper>


                        <Typography variant="caption" sx={{ opacity: 0.7 }}>
                            Selected tools: <b>{selectedToolKeys.size}</b>
                        </Typography>
                    </Stack>
                </TabPanel>

                {/* Endpoint Tab */}
                <TabPanel value={tab} index={2}>
                    <Stack spacing={2}>
                        <TextField
                            select
                            label="Method"
                            fullWidth
                            required
                            value={discoveryMethod}
                            onChange={(e) => setDiscoveryMethod(e.target.value)}
                        >
                            {DISCOVERY_METHODS.map((m) => (
                                <MenuItem key={m} value={m}>
                                    {m}
                                </MenuItem>
                            ))}
                        </TextField>

                        <TextField
                            select
                            label="Protocol"
                            fullWidth
                            required
                            value={discoveryProtocol}
                            onChange={(e) => setDiscoveryProtocol(e.target.value)}
                        >
                            {DISCOVERY_PROTOCOLS.map((p) => (
                                <MenuItem key={p} value={p}>
                                    {p}
                                </MenuItem>
                            ))}
                        </TextField>

                        <TextField
                            label="Endpoint (host[:port]/path)"
                            fullWidth
                            required
                            value={discoveryEndpoint}
                            onChange={(e) => setDiscoveryEndpoint(e.target.value)}
                            onBlur={() => setEndpointTouched(true)}
                            error={endpointError}
                            helperText={
                                endpointError
                                    ? "Invalid. Examples: api.example.com/v1/api/agents, api.example.com:8080/v1/api, localhost:8080/v1/api (no protocol, no query)."
                                    : "Required. Examples: api.example.com/v1/api/agents, myservice:8080/path, 10.0.0.5:8080/v1/api"
                            }
                            placeholder="api.popgenai.globallogic.com/v1/api/agents/agent1"
                        />

                        <TextField
                            label="Query Param"
                            fullWidth
                            required
                            value={discoveryQueryParam}
                            onChange={(e) => setDiscoveryQueryParam(e.target.value)}
                            onBlur={() => setQueryParamTouched(true)}
                            error={queryParamError}
                            helperText={queryParamError ? "Required." : "Required. Example: q"}
                            placeholder="q"
                        />

                        <Divider sx={{ my: 1 }} />

                        <Typography variant="subtitle2" sx={{ opacity: 0.85 }}>
                            Security
                        </Typography>

                        <TextField
                            label="Bearer Token (optional)"
                            fullWidth
                            value={bearerToken}
                            onChange={(e) => setBearerToken(e.target.value)}
                            onBlur={() => setBearerTouched(true)}
                            error={bearerError}
                            helperText={
                                bearerError
                                    ? "Invalid token format. Expected JWT: header.payload.signature (base64url). You may paste with or without 'Bearer '."
                                    : "Optional. Paste JWT (with or without 'Bearer '). If empty, Authorization header will NOT be sent."
                            }
                            placeholder="Bearer eyJhbGciOi... (optional)"
                        />

                        <Divider sx={{ my: 1 }} />

                        <Stack direction="row" spacing={2} justifyContent="flex-end" alignItems="center">
                            {!endpointNeedsVerification ? (
                                <Chip size="small" label="Verified" />
                            ) : endpointTest.status === "ok" ? (
                                <Chip size="small" label="Test OK" />
                            ) : endpointTest.status === "failed" ? (
                                <Chip size="small" label="Test FAILED" />
                            ) : (
                                <Chip size="small" label="Not tested" />
                            )}

                            <Button variant="outlined" disabled={testing || !endpointValid} onClick={handleTestConnection}>
                                {testing ? (
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <CircularProgress size={18} color="inherit" />
                                        <span>Testing...</span>
                                    </Stack>
                                ) : (
                                    "Test Connection"
                                )}
                            </Button>
                        </Stack>

                        <Collapse in={endpointTest.status === "failed" && !!endpointTest.error} sx={{ mt: 1 }}>
                            <Alert severity="error">{endpointTest.error}</Alert>
                        </Collapse>

                        <Collapse in={endpointTest.status === "ok" && !!endpointTest.result} sx={{ mt: 1 }}>
                            <Alert severity="success">
                                <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                                    <div>
                                        <b>Status:</b> {endpointTest.result?.status ?? "n/a"} — <b>Latency:</b>{" "}
                                        {endpointTest.result?.latencyMs ?? "n/a"} ms
                                    </div>
                                    {endpointTest.result?.message ? (
                                        <div>
                                            <b>Message:</b> {endpointTest.result.message}
                                        </div>
                                    ) : null}
                                    {endpointTest.result?.responseSnippet ? (
                                        <div style={{ whiteSpace: "pre-wrap" }}>
                                            <b>Response:</b> {endpointTest.result.responseSnippet}
                                        </div>
                                    ) : null}
                                </div>
                            </Alert>
                        </Collapse>
                    </Stack>
                </TabPanel>
            </DialogContent>

            <DialogActions>
                <Stack direction="row" spacing={2} alignItems="center" sx={{ width: "100%", justifyContent: "space-between" }}>
                    <Button onClick={onClose} color="inherit" disabled={saving}>
                        Cancel
                    </Button>

                    <Stack direction="row" spacing={2} alignItems="center">
                        {!isDirty ? (
                            <Typography variant="caption" sx={{ opacity: 0.7 }}>
                                Make a change to enable Save.
                            </Typography>
                        ) : endpointNeedsVerification ? (
                            <Typography variant="caption" sx={{ opacity: 0.7 }}>
                                Endpoint changed — run “Test Connection” to enable Save.
                            </Typography>
                        ) : null}

                        <Button onClick={handleSave} variant="contained" disabled={saveDisabled}>
                            {saving ? <CircularProgress size={22} color="inherit" /> : "Save"}
                        </Button>
                    </Stack>
                </Stack>
            </DialogActions>
        </Dialog>
    );
}

function useConfirmEmptyString(initialValue) {
    const [v, setV] = useState(initialValue ?? "");
    useEffect(() => {
        setV(initialValue ?? "");
    }, [initialValue]);
    return [v, setV];
}
