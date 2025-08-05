<#macro emailLayout>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
    <meta charset="UTF-8">
    <title>${subject}</title>
    <style>
        .logo { text-align: center; margin-bottom: 20px; }
        .content { font-family: Arial, sans-serif; }
    </style>
</head>
<body>
    <div class="logo">
        <img src="https://raw.githubusercontent.com/lvoxx/SRMS-backend/refs/heads/main/Images/SRMS-Logo.png" alt="SRMS Logo" width="150" />
    </div>
    <div class="content">
        <#nested>
        <div>
            <p style="font-size: 24px; color: #919191FF;">${kcSanitize(msg("systemMessage"))?no_esc}</p>
        </div>
    </div>  
</body>
</html>
</#macro>