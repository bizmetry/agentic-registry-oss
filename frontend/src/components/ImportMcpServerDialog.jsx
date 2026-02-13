import React, { useMemo, useRef, useState, useEffect } from "react";
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Stack, Typography, Alert, Collapse, Paper,
  IconButton, LinearProgress, RadioGroup, FormControlLabel, Radio,
  Card, CardContent, CardActions,
  TextField, InputAdornment
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import SearchIcon from "@mui/icons-material/Search";

// Funciones de importación y obtención de servidores
import { importMcpServerFromMcpCentral, getCentralMcpServers } from "../api/centralMcp";
import { importMcpServers } from "../api/mcpServers";

const PAGE_SIZE = 10;
const SEARCH_DEBOUNCE_MS = 350;

export function ImportMcpServerDialog({ open, onClose, onImported }) {
  const inputRef = useRef(null);
  const listRef = useRef(null);

  const [file, setFile] = useState(null);
  const [fileText, setFileText] = useState("");
  const [busy, setBusy] = useState(false);
  const [importType, setImportType] = useState("json"); // "json" o "mcp"
  const [selectedMcpServer, setSelectedMcpServer] = useState(null);

  const [mcpServers, setMcpServers] = useState([]);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

  // Infinite scroll
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1); // 1-based
  const [hasMore, setHasMore] = useState(true);
  const [importCompleted, setImportCompleted] = useState(false);

  // Search
  const [searchText, setSearchText] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  const parsedPayload = useMemo(() => {
    if (!fileText) return null;
    try {
      return JSON.parse(fileText);
    } catch {
      return null;
    }
  }, [fileText]);

  // Debounce search input
  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchText), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(t);
  }, [searchText]);

  // Reset list when switching to MCP mode or when search changes
  useEffect(() => {
    if (importType !== "mcp") return;

    setMcpServers([]);
    setPage(1);
    setHasMore(true);
    setSelectedMcpServer(null);
    // Nota: el fetch real lo hace el effect de abajo (page/importType/debouncedSearch)
  }, [importType, debouncedSearch]); // <- reset cuando cambia el search (debounced)

  // Fetch MCP servers with paging + search
  useEffect(() => {
    if (importType !== "mcp") return;

    const fetchMcpServers = async () => {
      setLoading(true);
      setError(null);
      try {
        const offset = (page - 1) * PAGE_SIZE; // ✅ correcto
        const servers = await getCentralMcpServers(offset, PAGE_SIZE, debouncedSearch);

        const list = Array.isArray(servers) ? servers : [];

        // Si page=1, reemplazamos; si no, append
        setMcpServers((prev) => (page === 1 ? list : [...prev, ...list]));

        // hasMore si la API devuelve "PAGE_SIZE" items
        setHasMore(list.length === PAGE_SIZE);
      } catch (err) {
        setError({ message: "Error fetching MCP servers" });
        setHasMore(false);
      } finally {
        setLoading(false);
      }
    };

    fetchMcpServers();
  }, [importType, page, debouncedSearch]);

  // Infinite scroll handler (solo MCP)
  useEffect(() => {
    if (importType !== "mcp") return;

    const el = listRef.current;
    if (!el) return;

    const handleScroll = (e) => {
      const listContainer = e.target;
      const isAtBottom =
        listContainer.scrollHeight - listContainer.scrollTop <= listContainer.clientHeight + 2;

      if (isAtBottom && hasMore && !loading) {
        setPage((prevPage) => prevPage + 1);
      }
    };

    el.addEventListener("scroll", handleScroll);
    return () => el.removeEventListener("scroll", handleScroll);
  }, [importType, hasMore, loading]);

  const pickFile = () => inputRef.current?.click();

  const onFileSelected = async (e) => {
    const f = e.target.files?.[0] || null;
    setResult(null);
    setError(null);
    setFile(f);
    setFileText("");

    if (!f) return;

    try {
      const text = await f.text();
      setFileText(text);
    } catch (err) {
      setError({ message: err?.message || "Could not read file." });
    }
  };

  const resetAndClose = () => {
    setFile(null);
    setFileText("");
    setBusy(false);
    setError(null);
    setResult(null);
    setImportCompleted(false);
    setSelectedMcpServer(null);

    // MCP state
    setMcpServers([]);
    setLoading(false);
    setPage(1);
    setHasMore(true);
    setSearchText("");
    setDebouncedSearch("");

    onClose?.();
  };

  const doImport = async () => {
    setError(null);
    setResult(null);

    if (importType === "json") {
      if (!file) {
        setError({ message: "Please select a .json file first." });
        return;
      }
      if (!parsedPayload) {
        setError({ message: "Invalid JSON. Please verify the exported file content." });
        return;
      }

      setBusy(true);
      try {
        const res = await importMcpServers(parsedPayload, { dryRun: false, upsert: true });
        setResult(res);
        onImported?.(res);
        setImportCompleted(true);
      } catch (e) {
        const data = e?.response?.data;
        setError({
          message: data?.error || data?.message || e?.message || "Import failed",
          details: data || null
        });
      } finally {
        setBusy(false);
      }
    } else if (importType === "mcp") {
      if (!selectedMcpServer) {
        setError({ message: "Please select an MCP Server." });
        return;
      }

      setBusy(true);
      try {
        // Nota: hoy tu import usa solo name. Si tu API soporta version, podés pasarla también.
        const res = await importMcpServerFromMcpCentral(selectedMcpServer.name, selectedMcpServer.version);
        setResult(res);
        onImported?.(res);
        setImportCompleted(true);
      } catch (e) {
        const data = e?.response?.data;
        setError({
          message: data?.error || data?.message || e?.message || "Import failed",
          details: data || null
        });
      } finally {
        setBusy(false);
      }
    }
  };

  return (
    <Dialog open={open} onClose={busy ? undefined : resetAndClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ pb: 1 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Stack spacing={0.25}>
            <Typography fontWeight={900}>Import MCP Servers</Typography>
            <Typography variant="body2" sx={{ opacity: 0.8 }}>
              Choose the import method
            </Typography>
          </Stack>

          <IconButton onClick={resetAndClose} disabled={busy}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Stack>
      </DialogTitle>

      {busy && <LinearProgress />}

      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Collapse in={!!error}>
            {error && (
              <Alert severity="error" variant="outlined">
                <Typography fontWeight={900}>Import failed</Typography>
                <Typography variant="body2" sx={{ mt: 0.5 }}>
                  {error.message}
                </Typography>
              </Alert>
            )}
          </Collapse>

          <Collapse in={!!result?.ok}>
            {result?.ok && (
              <Alert severity="success" variant="outlined">
                <Typography fontWeight={900}>Import completed</Typography>
                <Typography variant="body2" sx={{ mt: 0.5 }}>
                  Created: <b>{result.created ?? 0}</b> • Updated: <b>{result.updated ?? 0}</b> • Total:{" "}
                  <b>{result.servers?.length ?? 0}</b>
                </Typography>
              </Alert>
            )}
          </Collapse>

          <RadioGroup
            value={importType}
            onChange={(e) => setImportType(e.target.value)}
            sx={{ width: "100%" }}
          >
            <FormControlLabel value="json" control={<Radio />} label="Import from JSON file" />
            <FormControlLabel value="mcp" control={<Radio />} label="Import from OpenAI MCP Central Registry" />
          </RadioGroup>

          {importType === "json" && (
            <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
              <Stack spacing={1}>
                <Typography fontWeight={900}>Select file</Typography>
                <Typography variant="body2" sx={{ opacity: 0.8 }}>
                  Choose a <b>.json</b> file exported from the MCP Server registry.
                </Typography>

                <input
                  ref={inputRef}
                  type="file"
                  accept="application/json,.json"
                  style={{ display: "none" }}
                  onChange={onFileSelected}
                />

                <Stack direction="row" spacing={1} alignItems="center">
                  <Button variant="outlined" onClick={pickFile} disabled={busy || importCompleted}>
                    Choose file…
                  </Button>
                  <Typography variant="body2" sx={{ opacity: 0.85 }}>
                    {file ? <b>{file.name}</b> : "No file selected"}
                  </Typography>
                </Stack>

                {file && (
                  <Typography variant="caption" sx={{ opacity: 0.75 }}>
                    {parsedPayload ? "✅ JSON looks valid" : "⚠️ JSON invalid or unreadable"}
                  </Typography>
                )}
              </Stack>
            </Paper>
          )}

          {importType === "mcp" && (
            <>
              {/* ✅ Search bar */}
              <TextField
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                placeholder="Search MCP Servers…"
                size="small"
                disabled={busy}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" />
                    </InputAdornment>
                  ),
                }}
              />

              <div
                ref={listRef}
                id="mcp-server-list"
                style={{ maxHeight: 400, overflowY: "auto" }}
              >
                <Stack spacing={2} sx={{ pr: 0.5 }}>
                  {mcpServers.map((server) => {
                    const hasDiscoveryUrl = !!(server.discoveryUrl && String(server.discoveryUrl).trim().length > 0);
                    const isSelected =
                      selectedMcpServer?.name === server.name &&
                      selectedMcpServer?.version === server.version;

                    return (
                      <Card
                        key={server.name + server.version}
                        variant="outlined"
                        sx={{
                          cursor: hasDiscoveryUrl ? "pointer" : "not-allowed",
                          opacity: hasDiscoveryUrl ? 1 : 0.45,
                          filter: hasDiscoveryUrl ? "none" : "grayscale(100%)",
                          backgroundColor: isSelected
                            ? "#e3f2fd"
                            : (hasDiscoveryUrl ? "transparent" : "rgba(0,0,0,0.02)"),
                        }}
                        onClick={() => {
                          if (!hasDiscoveryUrl) return; // ✅ no selectable
                          setSelectedMcpServer({ name: server.name, version: server.version });
                        }}
                      >
                        <CardContent>
                          <Typography variant="h6">{server.name}</Typography>
                          <Typography variant="body2">{server.version}</Typography>
                          <Typography variant="body2">{server.description}</Typography>

                          <Typography variant="body2" sx={{ opacity: 0.7 }}>
                            {hasDiscoveryUrl ? server.discoveryUrl : "Missing discovery URL"}
                          </Typography>

                          {!hasDiscoveryUrl && (
                            <Typography variant="caption" sx={{ opacity: 0.8 }}>
                              This MCP server can’t be imported because it doesn’t expose a discovery URL.
                            </Typography>
                          )}
                        </CardContent>

                        <CardActions>
                          <Button
                            size="small"
                            disabled={!hasDiscoveryUrl || busy} // ✅ disabled if no discovery url
                            onClick={(e) => {
                              e.stopPropagation();
                              if (!hasDiscoveryUrl) return;
                              setSelectedMcpServer({ name: server.name, version: server.version });
                            }}
                          >
                            Select
                          </Button>
                        </CardActions>
                      </Card>
                    );
                  })}

                  {loading && (
                    <Typography variant="body2" sx={{ opacity: 0.75, textAlign: "center", py: 1 }}>
                      Loading…
                    </Typography>
                  )}

                  {!loading && mcpServers.length === 0 && (
                    <Typography variant="body2" sx={{ opacity: 0.75, textAlign: "center", py: 2 }}>
                      No servers found{debouncedSearch ? ` for "${debouncedSearch}"` : ""}.
                    </Typography>
                  )}
                </Stack>
              </div>
            </>
          )}


          {selectedMcpServer && (
            <Typography variant="body2" sx={{ mt: 2 }}>
              <b>Selected MCP Server:</b> {selectedMcpServer.name} - {selectedMcpServer.version}
            </Typography>
          )}
        </Stack>
      </DialogContent>

      <DialogActions>
        <Button onClick={resetAndClose} disabled={busy}>
          {importCompleted ? "Close" : "Cancel"}
        </Button>

        <Button
          variant="contained"
          onClick={doImport}
          disabled={busy || (importType === "json" && !file) || (importType === "mcp" && !selectedMcpServer)}
        >
          Import
        </Button>
      </DialogActions>
    </Dialog>
  );
}
